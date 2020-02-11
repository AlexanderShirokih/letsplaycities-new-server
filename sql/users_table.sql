CREATE TABLE `users` (
  `user_id` int(6) NOT NULL AUTO_INCREMENT,
  `auth_type` enum('nv','gl','vk','ok','fb') COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `reg_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_visit` datetime DEFAULT CURRENT_TIMESTAMP,
  `sn_uid` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `acc_id` varchar(8) COLLATE utf8mb4_bin DEFAULT NULL,
  `state` enum('unk','banned','ready','admin') COLLATE utf8mb4_bin NOT NULL,
  `firebase_token` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `avatar_hash` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`user_id`)
);
