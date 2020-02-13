INSERT INTO users(`auth_type`, `name`,`state`)
VALUES 
('nv', 'Joe', 'ready'),
('nv', 'Andy', 'ready'),
('nv', 'Mark', 'ready'),
('nv', 'Alex', 'ready'),
('nv', 'Foo', 'ready')
;

INSERT INTO friends(`sender_id`, `receiver_id`, `accepted`)
VALUES
(1, 2, 1),
(2, 3, 1),
(3, 4, 0),
(4, 1, 1),
(3, 5, 1);

INSERT INTO battle_history(`starter_id`, `invited_id`, `begin_time`, `duration`, `words_count`)
VALUES
(1, 2, NOW(), 10, 10),
(2, 1, NOW(), 10, 10),
(3, 1, NOW(), 10, 10),
(3, 4, NOW(), 10, 10),
(4, 1, NOW(), 10, 10),
(2, 1, NOW(), 10, 10);
