-- 07_views.sql

-- Unread-first list for a user+app (ready for the UI)
CREATE OR REPLACE VIEW v_app_notifications AS
SELECT *
FROM notifications
ORDER BY read ASC, ts DESC;  -- planner uses indexes; filter by user_id/app in query

-- Counters view across apps (if you prefer a single query over app_counters)
-- (This view is optional; app_counters table is already designed for fast reads.)
