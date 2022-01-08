-- Run the following commands to setup database
-- mysql -u root -p
-- mysql>source db_init.sql

/* Replace `password` with your real password */
CREATE DATABASE lps_data;
CREATE USER 'lps'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'lps'@'localhost';
GRANT ALL ON lps_data.* TO 'lps'@'localhost' IDENTIFIED BY 'password' WITH GRANT OPTION;
FLUSH PRIVILEGES;
USE lps_data;

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

CREATE TABLE IF NOT EXISTS `ForbiddenLogin`
(
    `value` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`value`)
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

CREATE TABLE IF NOT EXISTS `Country`
(
    `id`   INT         NOT NULL,
    `name` VARCHAR(64) NOT NULL,

    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `CountryGroup`
(
    `id`   INT         NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(28) NOT NULL,

    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `CountryGroupAssignments`
(
    `group_id`   INT NOT NULL,
    `country_id` INT NOT NULL,

    PRIMARY KEY (`group_id`, `country_id`),
    CONSTRAINT FK_CountryGroup
        FOREIGN KEY (`group_id`) REFERENCES CountryGroup (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_Country
        FOREIGN KEY (`country_id`) REFERENCES Country (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `RoomRequest`
(
    `id`               INT       NOT NULL AUTO_INCREMENT,
    `hash`             INT                DEFAULT NULL,
    `requester_id`     INT       NOT NULL,
    `country_group_id` INT                DEFAULT NULL,
    `creation_date`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`           ENUM ('NEW', 'ACCEPTED', 'DECLINED'),

    PRIMARY KEY (`id`),
    CONSTRAINT FK_RoomRequester
        FOREIGN KEY (`requester_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_RoomCountryGroup
        FOREIGN KEY (`country_group_id`) REFERENCES CountryGroup (`id`)
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

CREATE TABLE IF NOT EXISTS `City`
(
    `id`         INT         NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(64) NOT NULL,
    `country_id` INT         NOT NULL,

    UNIQUE (`name`, `country_id`),
    PRIMARY KEY (`id`),
    CONSTRAINT FK_CountryCodeForCity FOREIGN KEY (`country_id`)
        REFERENCES Country (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `CityEditRequest`
(
    `id`               INT      NOT NULL AUTO_INCREMENT,
    `owner_id`         INT      NOT NULL,
    `old_country_code` SMALLINT NOT NULL DEFAULT 0,
    `new_country_code` SMALLINT NOT NULL DEFAULT 0,
    `old_name`         VARCHAR(64)       DEFAULT NULL,
    `new_name`         VARCHAR(64)       DEFAULT NULL,
    `reason`           VARCHAR(64)       DEFAULT NULL,
    `verdict`          VARCHAR(64)       DEFAULT NULL,
    `status`           ENUM ('NEW', 'APPROVED', 'DECLINED'),

    PRIMARY KEY (`id`),
    CONSTRAINT CHK_OldCountryCodeValid CHECK (old_country_code >= 0),
    CONSTRAINT CHK_NewCountryCodeValid CHECK (new_country_code >= 0),
    CONSTRAINT FK_CityEditRequestOwner FOREIGN KEY (`owner_id`)
        REFERENCES User (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `Notification`
(
    `id`            INT         NOT NULL AUTO_INCREMENT,
    `target_id`     INT         NOT NULL,
    `title`         VARCHAR(30)          DEFAULT NULL,
    `content`       VARCHAR(64) NOT NULL,
    `creation_date` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    CONSTRAINT FK_NotificationOwner FOREIGN KEY (`target_id`)
        REFERENCES User (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `LastReadNotification`
(
    `user_id`      INT NOT NULL,
    `last_read_id` INT NOT NULL,

    PRIMARY KEY (`user_id`),
    CONSTRAINT FK_User FOREIGN KEY (`user_id`)
        REFERENCES User (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `Chat`
(
    `id`   INT                      NOT NULL AUTO_INCREMENT,
    `type` ENUM ('direct', 'multi') NOT NULL,
    `name` VARCHAR(64)              NULL DEFAULT NULL,

    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `Message`
(
    `id`         INT       NOT NULL AUTO_INCREMENT,
    `chat_id`    INT       NOT NULL,
    `author_id`  INT       NOT NULL,
    `content`    varchar(240) CHARACTER SET utf8 DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL              DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    CONSTRAINT FK_Chat FOREIGN KEY (`chat_id`)
        REFERENCES Chat (`id`) ON DELETE CASCADE,
    CONSTRAINT FK_Author FOREIGN KEY (`author_id`)
        REFERENCES User (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `ChatParticipant`
(
    `chat_id`              INT                                  NOT NULL,
    `participant_id`       INT                                  NOT NULL,
    `permission_level`     ENUM ('creator', 'admin', 'regular') NOT NULL,
    `last_read_message_id` INT                                  NULL DEFAULT NULL,

    PRIMARY KEY (`chat_id`, `participant_id`),
    CONSTRAINT FK_OwningChat FOREIGN KEY (`chat_id`)
        REFERENCES Chat (`id`) ON DELETE CASCADE,
    CONSTRAINT FK_Participant FOREIGN KEY (`participant_id`)
        REFERENCES User (`id`) ON DELETE CASCADE,
    CONSTRAINT FK_LastMessage FOREIGN KEY (`last_read_message_id`)
        REFERENCES Message (`id`) ON DELETE CASCADE
);
