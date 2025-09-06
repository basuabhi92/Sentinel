-- 02_types.sql
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'app_enum') THEN
    CREATE TYPE app_enum AS ENUM ('instagram','snapchat','gmail','x');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'delivery_enum') THEN
    CREATE TYPE delivery_enum AS ENUM ('silent','push','digest','email','webhook','drop');
  END IF;
END$$;
