/*
 *  Migration script
 */
ALTER TABLE users CHANGE user_id id int auto_increment;

-- Migration for 'Pictures':
ALTER TABLE pictures DROP COLUMN id;
ALTER TABLE pictures ADD PRIMARY KEY (owner_id);
ALTER TABLE pictures ADD CONSTRAINT FK_PictureOwner
	FOREIGN KEY (owner_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'Friends':
-- delete invalid entries
DELETE FROM friends WHERE 
	NOT EXISTS(SELECT * FROM users where users.id=friends.sender_id) 
	OR NOT EXISTS(SELECT * FROM users where users.id=friends.receiver_id);

ALTER TABLE friends DROP COLUMN id;
ALTER TABLE friends ADD PRIMARY KEY (sender_id, receiver_id);
ALTER TABLE friends ADD CONSTRAINT FK_FriendsSender
	FOREIGN KEY (sender_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE friends ADD CONSTRAINT FK_FriendsReceiver
	FOREIGN KEY (receiver_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'Banlist':
DELETE FROM banlist WHERE 
	NOT EXISTS(SELECT * FROM users where users.id=banlist.baner_id) 
	OR NOT EXISTS(SELECT * FROM users where users.id=banlist.banned_id);

ALTER TABLE banlist DROP COLUMN id;
ALTER TABLE banlist ADD PRIMARY KEY (baner_id, banned_id);
ALTER TABLE banlist ADD CONSTRAINT FK_BanlistBanner
	FOREIGN KEY (baner_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE banlist ADD CONSTRAINT FK_BanlistBanned
	FOREIGN KEY (banned_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;

-- Migration for 'BattleHistory':
-- Delete invalid entries:
DELETE FROM battle_history WHERE duration <= 0;

ALTER TABLE battle_history ADD CONSTRAINT CHK_Battle_Duration CHECK (duration>0);
ALTER TABLE battle_history ADD CONSTRAINT FK_BattleHistoryStarter
	FOREIGN KEY (starter_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE battle_history ADD CONSTRAINT FK_BattleHistoryInvited
	FOREIGN KEY (invited_id) REFERENCES users(id)
	ON DELETE CASCADE ON UPDATE CASCADE;

--Migration for 'Users' and table renaming:
ALTER TABLE users RENAME TO User;
ALTER TABLE banlist RENAME TO Banlist;
ALTER TABLE friends RENAME TO Friendship;
ALTER TABLE battle_history RENAME TO History;
ALTER TABLE pictures RENAME TO Picture;


ALTER TABLE User CHANGE acc_id access_hash  varchar(8) COLLATE utf8mb4_bin DEFAULT NULL;
ALTER TABLE User ADD role TINYINT(1) default 1 null;
UPDATE User SET role = 0 WHERE state = 'banned';
UPDATE User SET role = 2 WHERE state = 'admin';

alter table User
	add auth_type_i tinyint(1) not null;
UPDATE User SET auth_type_i = CASE
    WHEN auth_type = 'nv' THEN 0
    WHEN auth_type = 'gl' THEN 1
    WHEN auth_type = 'vk' THEN 2
    WHEN auth_type = 'ok' THEN 3
    WHEN auth_type = 'fb' THEN 4
END;
alter table User drop column auth_type;
alter table User change auth_type_i auth_type tinyint(1) not null;

ALTER TABLE User DROP COLUMN state;
