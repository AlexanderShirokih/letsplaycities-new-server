DROP TABLE IF EXISTS `User`;
CREATE TABLE IF NOT EXISTS `User`
(
    `id`             int(6)                                    NOT NULL AUTO_INCREMENT,
    `name`           varchar(64)            NOT NULL,
    `last_visit`     datetime                        DEFAULT CURRENT_TIMESTAMP,
    `avatar_hash`    varchar(32)  DEFAULT NULL,
    `sn_uid`         varchar(32) DEFAULT NULL,
    `access_hash`    varchar(8)   DEFAULT NULL,
    `firebase_token` varchar(200) DEFAULT NULL,
    `reg_date`       datetime                        DEFAULT CURRENT_TIMESTAMP,
    `auth_type`      tinyint(1)                                not null,
    `role`           TINYINT(1)                      DEFAULT 1 NOT NULL,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `Friendship`;
CREATE TABLE IF NOT EXISTS `Friendship` (
	`sender_id` INT(6) NOT NULL,
	`receiver_id` INT(6) NOT NULL,
	`accepted` BOOLEAN DEFAULT 0,
	`creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`sender_id`, `receiver_id`),
	CONSTRAINT FK_FriendsSender
	FOREIGN KEY (`sender_id`) REFERENCES User(`id`)
	ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FK_FriendsReceiver
	FOREIGN KEY (`receiver_id`) REFERENCES User(`id`)
	ON DELETE CASCADE ON UPDATE CASCADE
);

DROP TABLE IF EXISTS `History`;
CREATE TABLE IF NOT EXISTS `History`
(
    `id`          int       NOT NULL AUTO_INCREMENT,
    `starter_id`  INT(6)    NOT NULL,
    `invited_id`  INT(6)    NOT NULL,
    `begin_time`  TIMESTAMP NOT NULL,
    `duration`    INT(6)    NOT NULL,
    `words_count` INT(6)    NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT CHK_Battle_Duration CHECK (`duration` > 0),
    CONSTRAINT FK_BattleHistoryStarter
        FOREIGN KEY (`starter_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_BattleHistoryInvited
        FOREIGN KEY (`invited_id`) REFERENCES User (`id`)
            ON DELETE CASCADE ON UPDATE CASCADE
);



