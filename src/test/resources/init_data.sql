INSERT INTO User(`auth_type`, `name`,`role`)
VALUES 
(0, 'Joe', 1),
(0, 'Andy', 1),
(0, 'Mark', 1),
(0, 'Alex', 1),
(0, 'Foo', 1)
;

INSERT INTO Friendship(`sender_id`, `receiver_id`, `accepted`)
VALUES
(1, 2, 1),
(2, 3, 1),
(3, 4, 0),
(4, 1, 1),
(3, 5, 1);

INSERT INTO History(`starter_id`, `invited_id`, `begin_time`, `duration`, `words_count`)
VALUES
(1, 2, NOW(), 10, 10),
(2, 1, NOW(), 10, 10),
(3, 1, NOW(), 10, 10),
(3, 4, NOW(), 10, 10),
(4, 1, NOW(), 10, 10),
(2, 1, NOW(), 10, 10);
