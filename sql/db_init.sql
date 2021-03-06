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

CREATE TABLE IF NOT EXISTS `User`
(
    `id`             int(6)                                    NOT NULL AUTO_INCREMENT,
    `name`           varchar(64) COLLATE utf8mb4_bin           NOT NULL,
    `last_visit`     datetime                        DEFAULT CURRENT_TIMESTAMP,
    `avatar_hash`    varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
    `sn_uid`         varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
    `access_hash`    varchar(8) COLLATE utf8mb4_bin  DEFAULT NULL,
    `firebase_token` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
    `reg_date`       datetime                        DEFAULT CURRENT_TIMESTAMP,
    `auth_type`      tinyint(1)                                not null,
    `role`           TINYINT(1)                      DEFAULT 1 NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `Friendship`
(
    `sender_id`     INT(6)    NOT NULL,
    `receiver_id`   INT(6)    NOT NULL,
    `accepted`      BOOLEAN            DEFAULT 0,
    `creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`sender_id`, `receiver_id`),
    CONSTRAINT FK_FriendsSender
        FOREIGN KEY (`sender_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_FriendsReceiver
        FOREIGN KEY (`receiver_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `History`
(
    `id`          int       NOT NULL AUTO_INCREMENT,
    `starter_id`  INT(6)    NOT NULL,
    `invited_id`  INT(6)    NOT NULL,
    `begin_time`  TIMESTAMP NOT NULL,
    `duration`    INT(6)    NOT NULL,
    `words_count` INT(6)    NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT CHK_Battle_Duration CHECK (duration > 0),
    CONSTRAINT FK_BattleHistoryStarter
        FOREIGN KEY (`starter_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_BattleHistoryInvited
        FOREIGN KEY (`invited_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `Banlist`
(
    `baner_id`  INT(6) NOT NULL, /*User which sends ban*/
    `banned_id` INT(6) NOT NULL, /*User which being banned*/
    PRIMARY KEY (baner_id, banned_id),
    CONSTRAINT FK_BanlistBanner
        FOREIGN KEY (baner_id) REFERENCES User (id)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_BanlistBanned
        FOREIGN KEY (banned_id) REFERENCES User (id)
            ON DELETE CASCADE ON UPDATE CASCADE
) character set UTF8mb4
  collate utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `Picture`
(
    `owner_id` INT(6)     NOT NULL,
    `image`    BLOB       NOT NULL,
    `type`     TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`owner_id`),
    CONSTRAINT FK_PictureOwner FOREIGN KEY (`owner_id`)
        REFERENCES User (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);