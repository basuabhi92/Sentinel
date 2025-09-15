CREATE TABLE IF NOT EXISTS apps (
  id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        TEXT NOT NULL UNIQUE,
  logo_url    TEXT,
  metadata    JSONB,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed: GitHub
INSERT INTO apps (id, name, logo_url, metadata, created_at, updated_at)
VALUES (
  DEFAULT,
  'GitHub',
  'https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png',
  '{
    "brandColor": "#24292e",
    "website": "https://github.com",
    "docs": "https://docs.github.com",
    "categories": ["developer-tools","version-control"]
  }'::jsonb,
  now(),
  now()
);

ALTER TABLE integrations
  ADD COLUMN app_id   INT NOT NULL;

ALTER TABLE integrations
  DROP CONSTRAINT integrations_pkey;

ALTER TABLE integrations
  DROP COLUMN app;

ALTER TABLE integrations
  ADD CONSTRAINT integrations_pkey PRIMARY KEY (user_id, app_id),
  ADD CONSTRAINT integrations_app_fk
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS integrations_app_id_idx ON integrations (app_id);

ALTER TABLE app_counters
  ADD COLUMN app_id INT NOT NULL;

ALTER TABLE app_counters
  DROP CONSTRAINT app_counters_pkey;

ALTER TABLE app_counters
  DROP COLUMN app;

ALTER TABLE app_counters
  ADD CONSTRAINT app_counters_pkey PRIMARY KEY (user_id, app_id),
  ADD CONSTRAINT app_counters_integration_fk
    FOREIGN KEY (user_id, app_id) REFERENCES integrations(user_id, app_id) ON DELETE CASCADE;
