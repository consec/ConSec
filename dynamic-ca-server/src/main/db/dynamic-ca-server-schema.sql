SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `dynamic_ca_server` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;
USE `dynamic_ca_server` ;

-- -----------------------------------------------------
-- Table `dynamic_ca_server`.`ca`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `dynamic_ca_server`.`ca` (
  `uid` VARCHAR(36) NOT NULL ,
  `seq_num` INT NOT NULL ,
  `name` VARCHAR(100) NULL ,
  `private_key` TEXT NULL ,
  `certificate` TEXT NULL ,
  `cert_sn_counter` INT NOT NULL DEFAULT 1 ,
  `crl_counter` INT NOT NULL DEFAULT 1 ,
  PRIMARY KEY (`uid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `dynamic_ca_server`.`cert`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `dynamic_ca_server`.`cert` (
  `sn` INT NOT NULL ,
  `ca_uid` VARCHAR(36) NOT NULL ,
  `private_key` TEXT NULL ,
  `certificate` TEXT NULL ,
  `revoked` TINYINT(1)  NOT NULL DEFAULT false ,
  `revocation_date` TIMESTAMP NULL ,
  PRIMARY KEY (`sn`, `ca_uid`) ,
  INDEX `fk_cert_ca1` (`ca_uid` ASC) ,
  CONSTRAINT `fk_cert_ca1`
    FOREIGN KEY (`ca_uid` )
    REFERENCES `dynamic_ca_server`.`ca` (`uid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `sequence`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `SEQUENCE` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `SEQUENCE` VALUES ('SEQ_GEN',50);


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
