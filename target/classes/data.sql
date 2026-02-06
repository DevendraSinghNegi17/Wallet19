INSERT INTO asset (code, limited_supply) VALUES
('GOLD', false),
('DIAMOND', true),
('POINTS', false)
ON CONFLICT (code) DO NOTHING;

INSERT INTO wallet (user_id, asset, balance, version) VALUES
('SYSTEM', 'DIAMOND', 1000000, 0),
('SYSTEM', 'GOLD', 0, 0),
('SYSTEM', 'POINTS', 0, 0)
ON CONFLICT (user_id, asset) DO NOTHING;

INSERT INTO wallet (user_id, asset, balance, version) VALUES
('user1', 'GOLD', 1000, 0),
('user1', 'DIAMOND', 10, 0),
('user1', 'POINTS', 100, 0)
ON CONFLICT (user_id, asset) DO NOTHING;

INSERT INTO wallet (user_id, asset, balance, version) VALUES
('user2', 'GOLD', 500, 0),
('user2', 'DIAMOND', 5, 0),
('user2', 'POINTS', 50, 0)
ON CONFLICT (user_id, asset) DO NOTHING;
