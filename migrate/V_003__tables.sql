-- 03_tables.sql
SET timezone = 'UTC';

-- USERS
CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           CITEXT UNIQUE NOT NULL,
  password_hash   TEXT NOT NULL,
  name            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- INTEGRATIONS (per user+app)
CREATE TABLE IF NOT EXISTS integrations (
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  app             app_enum NOT NULL,
  provider_user_id TEXT,
  access_token_enc BYTEA NOT NULL,
  refresh_token_enc BYTEA,
  scopes          TEXT[],
  expires_at      TIMESTAMPTZ,
  webhook_status  TEXT,                   -- e.g., 'verified', 'pending', 'disabled'
  cursor_json     JSONB,                  -- polling cursors (since_id, etc.)
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  cursor_json     JSONB,
  PRIMARY KEY (user_id, app)
);

-- OAUTH SESSIONS (short-lived)
CREATE TABLE IF NOT EXISTS oauth_sessions (
  state           TEXT PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  app             app_enum NOT NULL,
  code_verifier   TEXT NOT NULL,
  redirect_uri    TEXT NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at      TIMESTAMPTZ NOT NULL
);

-- RULES
CREATE TABLE IF NOT EXISTS rules (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  app             app_enum NOT NULL,
  enabled         BOOLEAN NOT NULL DEFAULT TRUE,
  priority        INT NOT NULL DEFAULT 50,   -- lower = earlier
  when_json       JSONB NOT NULL,            -- your match object
  then_json       JSONB NOT NULL,            -- { action, labels?, ... }
  retention_ttl_hours INT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- APP COUNTERS (per user+app)
CREATE TABLE IF NOT EXISTS app_counters (
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  app             app_enum NOT NULL,
  unread          INT  NOT NULL DEFAULT 0,
  last_ts         TIMESTAMPTZ,
  last_seen_at    TIMESTAMPTZ,
  PRIMARY KEY (user_id, app)
);

-- DEVICES (push tokens)
CREATE TABLE IF NOT EXISTS devices (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  platform        TEXT NOT NULL CHECK (platform IN ('ios','android','web')),
  token_enc       BYTEA NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- NOTIFICATIONS (partitioned by hash(user_id) for write scalability)
-- Parent
CREATE TABLE IF NOT EXISTS notifications (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  app             app_enum NOT NULL,
  ts              TIMESTAMPTZ NOT NULL,              -- provider timestamp
  title           TEXT NOT NULL,
  preview         TEXT,
  read            BOOLEAN NOT NULL DEFAULT FALSE,
  labels          TEXT[] NOT NULL DEFAULT '{}',
  delivery        delivery_enum NOT NULL,            -- silent|push|digest|email|webhook|drop
  priority_score  NUMERIC(5,3),
  provider        TEXT NOT NULL,                     -- e.g., 'gmail','slack'
  provider_msg_id TEXT NOT NULL,                     -- idempotency key part
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  purge_after     TIMESTAMPTZ,                       -- when eligible for deletion
  UNIQUE (user_id, app, provider, provider_msg_id)
) PARTITION BY HASH (user_id);

-- Child partitions (8-way, tune as needed)
DO $$
BEGIN
  FOR i IN 0..7 LOOP
    EXECUTE format($$
      CREATE TABLE IF NOT EXISTS notifications_p%s
      PARTITION OF notifications FOR VALUES WITH (MODULUS 8, REMAINDER %s);
    $$, i, i);
  END LOOP;
END$$;

-- DIGESTS (optional, for batched summaries)
CREATE TABLE IF NOT EXISTS digests (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  app             app_enum NOT NULL,
  bucket          TIMESTAMPTZ NOT NULL,  -- hour/day bucket boundary
  items           JSONB NOT NULL,        -- [{id,title,ts}, ...]
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- Event outbox (queue) -----------------------------------------
CREATE TABLE IF NOT EXISTS event_outbox (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  topic           TEXT NOT NULL,                  -- e.g., 'notification.ingested'
  user_id         UUID,                           -- for sharding/metrics
  payload         JSONB NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  available_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  attempts        INT NOT NULL DEFAULT 0,
  max_attempts    INT NOT NULL DEFAULT 8,
  last_error      TEXT,
  status          TEXT NOT NULL DEFAULT 'pending' -- pending|processing|done|dead
);
CREATE INDEX IF NOT EXISTS event_outbox_ready_idx
  ON event_outbox (status, available_at, created_at);

-- Outbox processing lease (advisory-style via UPDATE ... FOR UPDATE SKIP LOCKED)
-- No table needed; we’ll just use SKIP LOCKED.

-- Optional: per-consumer offsets (only if you want at-least-once *per named consumer*)
CREATE TABLE IF NOT EXISTS event_consumer_offsets (
  consumer       TEXT NOT NULL,                   -- e.g., 'ruleengine','materializer'
  last_event_id  UUID,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(consumer)
);
