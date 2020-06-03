-- Run the following commands to setup database
-- mysql -u root -p
-- mysql>source db_init.sql

/* Replace `password` with your real password */
CREATE DATABASE lps_data;
CREATE USER 'lps'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON * . * TO 'lps'@'localhost';
GRANT ALL ON lps_data.* TO 'lps'@'localhost' IDENTIFIED BY 'password' WITH GRANT OPTION;
FLUSH PRIVILEGES;
CONNECT lps_data;

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int(6) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `auth_type` enum('nv','gl','vk','ok','fb') COLLATE utf8mb4_bin NOT NULL,
  `last_visit` datetime DEFAULT CURRENT_TIMESTAMP,
  `avatar_hash` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`user_id`)
);

CREATE TABLE IF NOT EXISTS `friends` (
	`sender_id` INT(6) NOT NULL,
	`receiver_id` INT(6) NOT NULL,
	`accepted` BOOLEAN DEFAULT 0,
	`creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`sender_id`, `receiver_id`),
	CONSTRAINT FK_FriendsSender
	FOREIGN KEY (`sender_id`) REFERENCES users(`user_id`)
	ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_FriendsReceiver
	FOREIGN KEY (`receiver_id`) REFERENCES users(`user_id`)
	ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `battle_history` (
	`starter_id` INT(6) NOT NULL,
	`invited_id` INT(6) NOT NULL,
	`begin_time` TIMESTAMP NOT NULL,
	`duration` INT(6) NOT NULL,
	`words_count` INT(6) NOT NULL,
	CONSTRAINT CHK_Battle_Duration CHECK (duration>0),
	CONSTRAINT FK_BattleHistoryStarter
	FOREIGN KEY (`starter_id`) REFERENCES users(`user_id`)
	ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_BattleHistoryInvited
	FOREIGN KEY (`invited_id`) REFERENCES users(`user_id`)
	ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `banlist` (
	`baner_id` INT(6) NOT NULL, /*User which sends ban*/
	`banned_id` INT(6) NOT NULL, /*User which being banned*/
	PRIMARY KEY (baner_id, banned_id),
	CONSTRAINT FK_BanlistBanner
	FOREIGN KEY (baner_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_BanlistBanned
	FOREIGN KEY (banned_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE
) character set UTF8mb4 collate utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `pictures` (
	`owner_id` INT(6) NOT NULL,
	`image` BLOB NOT NULL,
	`type` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`owner_id`),
    CONSTRAINT FK_PictureOwner FOREIGN KEY (`owner_id`) 
    REFERENCES users(`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
);
 
CREATE TABLE IF NOT EXISTS `AuthData` (
  `user_id` int(6) NOT NULL,
  `sn_uid` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `access_hash` varchar(8) COLLATE utf8mb4_bin DEFAULT NULL,
  `firebase_token` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `reg_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `state` enum('banned','ready','admin') COLLATE utf8mb4_bin NOT NULL DEFAULT 'ready',
  PRIMARY KEY (`user_id`),
  CONSTRAINT FK_AuthData_UserId
  FOREIGN KEY (user_id) REFERENCES users(user_id)
  ON DELETE CASCADE ON UPDATE CASCADE;
);
