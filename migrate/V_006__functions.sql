-- 05_functions.sql

-- Mark all read (optionally before a timestamp) and schedule purge
-- Returns number of rows affected.
CREATE OR REPLACE FUNCTION mark_all_read_and_purge(
  p_user_id UUID,
  p_app     app_enum,
  p_before  TIMESTAMPTZ DEFAULT NULL,         -- optional: only clear older
  p_purge_delay INTERVAL DEFAULT '15 minutes' -- how long to keep after read
) RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INTEGER := 0;
BEGIN
  -- Update notifications to read=true and set purge_after.
  IF p_before IS NULL THEN
    UPDATE notifications
       SET read = TRUE,
           purge_after = now() + p_purge_delay
     WHERE user_id = p_user_id
       AND app     = p_app
       AND read    = FALSE;
    GET DIAGNOSTICS v_count = ROW_COUNT;
  ELSE
    UPDATE notifications
       SET read = TRUE,
           purge_after = now() + p_purge_delay
     WHERE user_id = p_user_id
       AND app     = p_app
       AND read    = FALSE
       AND ts      <= p_before;
    GET DIAGNOSTICS v_count = ROW_COUNT;
  END IF;

  -- Reset counters (unread -> 0). This is idempotent.
  UPDATE app_counters
     SET unread = 0,
         last_seen_at = now()
   WHERE user_id = p_user_id
     AND app     = p_app;

  RETURN v_count;
END$$;

-- Immediate hard delete (use sparingly; better to rely on purge_after)
CREATE OR REPLACE FUNCTION purge_now(
  p_user_id UUID,
  p_app     app_enum,
  p_scope   TEXT,               -- 'all' | 'read' | 'olderThan'
  p_ts      TIMESTAMPTZ DEFAULT NULL
) RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INTEGER := 0;
BEGIN
  IF p_scope = 'all' THEN
    DELETE FROM notifications WHERE user_id = p_user_id AND app = p_app;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    UPDATE app_counters SET unread = 0 WHERE user_id = p_user_id AND app = p_app;

  ELSIF p_scope = 'read' THEN
    DELETE FROM notifications WHERE user_id = p_user_id AND app = p_app AND read = TRUE;
    GET DIAGNOSTICS v_count = ROW_COUNT;

  ELSIF p_scope = 'olderThan' AND p_ts IS NOT NULL THEN
    DELETE FROM notifications WHERE user_id = p_user_id AND app = p_app AND ts < p_ts;
    GET DIAGNOSTICS v_count = ROW_COUNT;

  ELSE
    RAISE EXCEPTION 'Invalid scope or missing timestamp';
  END IF;

  RETURN v_count;
END$$;

-- Nightly job: purge by purge_after
CREATE OR REPLACE FUNCTION purge_expired_notifications()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_count INTEGER;
BEGIN
  DELETE FROM notifications WHERE purge_after IS NOT NULL AND purge_after < now();
  GET DIAGNOSTICS v_count = ROW_COUNT;
  RETURN v_count;
END$$;
