DROP TABLE friends;
CREATE TABLE `friends` (
    `id` INT(11) NOT NULL primary key auto_increment,
	`sender_id` INT(6) NOT NULL,
	`receiver_id` INT(6) NOT NULL,
	`accepted` BOOLEAN DEFAULT 0,
	`creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE users;
CREATE TABLE `users` (
  `user_id` int(6) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `auth_type` enum('nv','gl','vk','ok','fb') NOT NULL,
  `name` varchar(64) NOT NULL,
  `reg_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_visit` datetime DEFAULT CURRENT_TIMESTAMP,
  `sn_uid` varchar(32) DEFAULT NULL,
  `acc_id` varchar(8)  DEFAULT NULL,
  `state` enum('unk','banned','ready','admin')  NOT NULL,
  `firebase_token` varchar(200) DEFAULT NULL,
  `avatar_hash` varchar(32) DEFAULT NULL
);

DROP TABLE battle_history;
CREATE TABLE IF NOT EXISTS `battle_history` (
    `id` INT(11) NOT NULL primary key auto_increment,
	`starter_id` INT(6) NOT NULL,
	`invited_id` INT(6) NOT NULL,
	`begin_time` TIMESTAMP NOT NULL,
	`duration` INT(6) NOT NULL,
	`words_count` INT(6) NOT NULL
);



