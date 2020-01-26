CREATE TABLE IF NOT EXISTS `pictures` (
    `id` INT(11) NOT NULL primary key auto_increment,
	`owner_id` INT(6) NOT NULL,
	`image` BLOB NOT NULL
);
