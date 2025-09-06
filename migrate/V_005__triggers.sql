-- 04_triggers.sql

-- Keep updated_at fresh
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS trg_integrations_updated ON integrations;
CREATE TRIGGER trg_integrations_updated
BEFORE UPDATE ON integrations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_rules_updated ON rules;
CREATE TRIGGER trg_rules_updated
BEFORE UPDATE ON rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_devices_updated ON devices;
CREATE TRIGGER trg_devices_updated
BEFORE UPDATE ON devices
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Maintain app_counters on notification INSERT/UPDATE/DELETE
-- NOTE: Keep triggers minimal; heavy logic should live in services.
CREATE OR REPLACE FUNCTION notif_after_insert()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- Only count if not read at insert time
  IF NEW.read = FALSE THEN
    INSERT INTO app_counters (user_id, app, unread, last_ts)
    VALUES (NEW.user_id, NEW.app, 1, NEW.ts)
    ON CONFLICT (user_id, app)
    DO UPDATE SET
      unread = app_counters.unread + 1,
      last_ts = GREATEST(app_counters.last_ts, EXCLUDED.last_ts);
  ELSE
    INSERT INTO app_counters (user_id, app, unread, last_ts)
    VALUES (NEW.user_id, NEW.app, 0, NEW.ts)
    ON CONFLICT (user_id, app)
    DO UPDATE SET
      last_ts = GREATEST(app_counters.last_ts, EXCLUDED.last_ts);
  END IF;
  RETURN NULL;
END$$;

DROP TRIGGER IF EXISTS trg_notif_after_insert ON notifications;
CREATE TRIGGER trg_notif_after_insert
AFTER INSERT ON notifications
FOR EACH ROW EXECUTE FUNCTION notif_after_insert();

CREATE OR REPLACE FUNCTION notif_after_update()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- If read status flips false -> true, decrement unread
  IF OLD.read = FALSE AND NEW.read = TRUE THEN
    UPDATE app_counters
      SET unread = GREATEST(unread - 1, 0)
      WHERE user_id = NEW.user_id AND app = NEW.app;
  END IF;
  -- Keep last_ts fresh if ts moved forward (rare)
  IF NEW.ts > COALESCE((SELECT last_ts FROM app_counters WHERE user_id = NEW.user_id AND app = NEW.app), 'epoch') THEN
    UPDATE app_counters
      SET last_ts = NEW.ts
      WHERE user_id = NEW.user_id AND app = NEW.app;
  END IF;
  RETURN NULL;
END$$;

DROP TRIGGER IF EXISTS trg_notif_after_update ON notifications;
CREATE TRIGGER trg_notif_after_update
AFTER UPDATE ON notifications
FOR EACH ROW EXECUTE FUNCTION notif_after_update();

-- Optional: if you ever delete unread items, decrement counter
CREATE OR REPLACE FUNCTION notif_after_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.read = FALSE THEN
    UPDATE app_counters
      SET unread = GREATEST(unread - 1, 0)
      WHERE user_id = OLD.user_id AND app = OLD.app;
  END IF;
  RETURN NULL;
END$$;

DROP TRIGGER IF EXISTS trg_notif_after_delete ON notifications;
CREATE TRIGGER trg_notif_after_delete
AFTER DELETE ON notifications
FOR EACH ROW EXECUTE FUNCTION notif_after_delete();
