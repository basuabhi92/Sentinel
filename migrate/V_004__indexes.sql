-- 04_indexes.sql

-- RULES: fast fetch by user, priority
CREATE INDEX IF NOT EXISTS rules_user_pri_idx
  ON rules(user_id, priority ASC) WHERE enabled = TRUE;

-- NOTIFICATIONS: unread-first + ts desc (partitioned: index on parent propagates)
CREATE INDEX IF NOT EXISTS notifications_user_app_read_ts_idx
  ON notifications (user_id, app, read, ts DESC);

-- Partial index for unread (very common)
CREATE INDEX IF NOT EXISTS notifications_unread_idx
  ON notifications (user_id, app, ts DESC)
  WHERE read = FALSE;

-- APP COUNTERS quick scan
-- (PK already covers user_id+app; add composite if you frequently fetch all apps for user)
-- Optional: none needed beyond PK.

-- OAUTH SESSIONS: expiry cleanup
CREATE INDEX IF NOT EXISTS oauth_sessions_expires_idx
  ON oauth_sessions(expires_at);

-- INTEGRATIONS: quick cross-user ops per app (optional)
CREATE INDEX IF NOT EXISTS integrations_app_idx
  ON integrations(app);
