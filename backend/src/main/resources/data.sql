-- 本地 / 集成测试（H2）种子数据
INSERT INTO operation_records (module_name, owner_name, status, metric)
SELECT '剧本库与DM管理', '运营组', 'ready', '100%'
WHERE NOT EXISTS (SELECT 1 FROM operation_records);

INSERT INTO scripts (id, name, genre, difficulty, duration_minutes, min_players, max_players, dm_required, description) VALUES
  (1, '年轮', '情感还原', '困难', 300, 4, 6, TRUE, '跨时空情感还原本，层层反转，适合熟车体验。'),
  (2, '窗边的女人', '推理硬核', '中等', 240, 5, 7, TRUE, '本格推理本，重证据链与时间线，适合推理爱好者。'),
  (3, '欢乐大富翁', '欢乐机制', '简单', 180, 6, 8, FALSE, '轻松欢乐机制本，新人友好，适合团建拼车。'),
  (4, '雾起云浮', '恐怖沉浸', '困难', 300, 4, 6, TRUE, '沉浸式恐怖本，氛围拉满，胆大者入。'),
  (5, '星夜双人舞', '情感还原', '中等', 180, 2, 8, TRUE, '支持双人开场的灵活拼车本，小团也能开。');

INSERT INTO players (id, name, phone, member_level) VALUES
  (1, '林晓彤', '13800000001', '黄金'),
  (2, '陈志远', '13800000002', '白银'),
  (3, '苏婉清', '13800000003', '青铜'),
  (4, '赵启铭', '13800000004', '钻石'),
  (5, '周沐沐', '13800000005', '白银'),
  (6, '何子昂', '13800000006', '青铜');

INSERT INTO sessions (id, script_id, dm_name, start_time, capacity, status) VALUES
  (1, 2, 'DM 老柯', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 6, 'SCHEDULED'),
  (2, 1, 'DM 小鹿', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 4, 'SCHEDULED'),
  (3, 3, NULL, DATEADD('HOUR', 3, CURRENT_TIMESTAMP), 8, 'SCHEDULED'),
  (4, 4, 'DM 老柯', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 6, 'STARTED');

INSERT INTO registrations (session_id, player_id, status, waitlist_position) VALUES
  (1, 1, 'REGISTERED', NULL),
  (1, 2, 'REGISTERED', NULL),
  (1, 3, 'REGISTERED', NULL),
  (1, 4, 'REGISTERED', NULL),
  (2, 1, 'REGISTERED', NULL),
  (2, 2, 'REGISTERED', NULL),
  (2, 5, 'REGISTERED', NULL),
  (2, 6, 'REGISTERED', NULL),
  (2, 3, 'WAITLISTED', 1),
  (2, 4, 'WAITLISTED', 2);

ALTER TABLE scripts ALTER COLUMN id RESTART WITH 100;
ALTER TABLE players ALTER COLUMN id RESTART WITH 100;
ALTER TABLE sessions ALTER COLUMN id RESTART WITH 100;
ALTER TABLE registrations ALTER COLUMN id RESTART WITH 100;
