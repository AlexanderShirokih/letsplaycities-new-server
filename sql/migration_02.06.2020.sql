/*
 *  Migration script
 */

-- Migration for 'Pictures':
ALTER TABLE pictures DROP COLUMN id;
ALTER TABLE pictures ADD PRIMARY KEY (owner_id);
ALTER TABLE pictures ADD CONSTRAINT FK_PictureOwner
	FOREIGN KEY (owner_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'Friends':
-- delete invalid entries
DELETE FROM friends f WHERE 
	NOT EXISTS(SELECT * FROM users where user_id=f.sender_id) 
	OR NOT EXISTS(SELECT * FROM users where user_id=f.receiver_id);

ALTER TABLE friends DROP COLUMN id;
ALTER TABLE friends ADD PRIMARY KEY (sender_id, receiver_id);
ALTER TABLE friends ADD CONSTRAINT FK_FriendsSender
	FOREIGN KEY (sender_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE friends ADD CONSTRAINT FK_FriendsReceiver
	FOREIGN KEY (receiver_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'Banlist':
-- Delete duplicates:
DELETE b1 FROM banlist b1
INNER JOIN banlist b2 
WHERE 
    b1.id < b2.id AND 
    b1.baner_id = b2.baner_id OR b1.banned_id = b2.banned_id;
-- Delete invalid entries:
DELETE FROM banlist b WHERE 
	NOT EXISTS(SELECT * FROM users where user_id=b.baner_id) 
	OR NOT EXISTS(SELECT * FROM users where user_id=b.banned_id);

ALTER TABLE banlist DROP COLUMN id;
ALTER TABLE banlist ADD PRIMARY KEY (baner_id, banned_id);
ALTER TABLE banlist ADD CONSTRAINT FK_BanlistBanner
	FOREIGN KEY (baner_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE banlist ADD CONSTRAINT FK_BanlistBanned
	FOREIGN KEY (banned_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'BattleHistory':
-- Delete invalid entries:
DELETE FROM battle_history WHERE duration <= 0;

ALTER TABLE battle_history ADD CONSTRAINT CHK_Battle_Duration CHECK (duration>0);
ALTER TABLE battle_history ADD CONSTRAINT FK_BattleHistoryStarter
	FOREIGN KEY (starter_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE battle_history ADD CONSTRAINT FK_BattleHistoryInvited
	FOREIGN KEY (invited_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;

--Migration for 'Users':
CREATE TABLE IF NOT EXISTS `AuthData` (
  `user_id` int(6) NOT NULL,
  `sn_uid` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `acc_id` varchar(8) COLLATE utf8mb4_bin DEFAULT NULL,
  `firebase_token` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `reg_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `auth_type` enum('nv','gl','vk','ok','fb') COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`user_id`)
);

INSERT INTO AuthData(user_id, sn_uid, acc_id, firebase_token, reg_date, auth_type)
SELECT user_id, sn_uid, acc_id, firebase_token, reg_date, auth_type FROM users;

ALTER TABLE users DROP COLUMN sn_uid;
ALTER TABLE users DROP COLUMN acc_id;
ALTER TABLE users DROP COLUMN firebase_token;
ALTER TABLE users DROP COLUMN reg_date;
ALTER TABLE users DROP COLUMN auth_type;
ALTER TABLE AuthData CHANGE acc_id access_hash  varchar(8) COLLATE utf8mb4_bin DEFAULT NULL;

ALTER TABLE AuthData ADD CONSTRAINT FK_AuthData_UserId
	FOREIGN KEY (user_id) REFERENCES users(user_id)
	ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE users RENAME TO User;
ALTER TABLE banlist RENAME TO Banlist;
ALTER TABLE friends RENAME TO Friendship;
ALTER TABLE battle_history RENAME TO History;
ALTER TABLE pictures RENAME TO Picture;
