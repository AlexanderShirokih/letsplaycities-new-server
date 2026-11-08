-- MySQL dump 10.16  Distrib 10.1.48-MariaDB (synthetic test fixture)

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `id` int(11) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` VALUES (1,'Вася \'Пупкин\'',-5,1.5),(2,'Test Two',0,NULL),(3,'',7,2);
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;

INSERT INTO `Other` VALUES (100,'secret-ish synthetic'),(101,'x');
INSERT INTO `Picture` VALUES (1,'\0\'\\\n\Z),(�'),(2,'a;b\nINSERT INTO `User` VALUES (9,1);\n),(3,\'z'),(3,'');
INSERT INTO `History` VALUES (1,NULL,'NULL'),(2,10,NULL);
