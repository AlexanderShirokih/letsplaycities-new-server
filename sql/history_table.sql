CREATE TABLE IF NOT EXISTS `battle_history` (
    `id` INT(11) NOT NULL primary key auto_increment,
	`starter_id` INT(6) NOT NULL,
	`invited_id` INT(6) NOT NULL,
	`begin_time` TIMESTAMP NOT NULL,
	`duration` INT(6) NOT NULL,
	`words_count` INT(6) NOT NULL
);
