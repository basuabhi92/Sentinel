-- 07_seed.sql

-- demo user
INSERT INTO users (id, email, password_hash, name)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'demo@example.com', 'HASHED', 'Demo User')
ON CONFLICT DO NOTHING;

-- counters seed
INSERT INTO app_counters(user_id, app, unread, last_ts)
VALUES
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','gmail',0,NULL),
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','instagram',0,NULL),
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','snapchat',0,NULL),
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','x',0,NULL)
ON CONFLICT DO NOTHING;

-- sample notifications (unread)
INSERT INTO notifications(user_id, app, ts, title, preview, read, labels, delivery, provider, provider_msg_id)
VALUES
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','gmail',    now()-interval '5 min','Build failed — CI','Pipeline #123',FALSE,'{work,ci}','silent','gmail','gm_1'),
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','instagram',now()-interval '2 min','New like','@alex liked your post',FALSE,'{social}','silent','instagram','ig_1');
