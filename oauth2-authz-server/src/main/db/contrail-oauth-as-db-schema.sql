SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `contrail_oauth_as` DEFAULT CHARACTER SET latin1 ;
USE `contrail_oauth_as` ;

-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`organization`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`organization` (
  `id` INT NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(100) NULL ,
  PRIMARY KEY (`id`) )
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`client`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`client` (
  `id` INT(11) NOT NULL AUTO_INCREMENT ,
  `client_id` VARCHAR(100) NOT NULL ,
  `name` VARCHAR(100) NOT NULL ,
  `callback_uri` VARCHAR(256) NULL ,
  `client_secret` VARCHAR(256) NOT NULL ,
  `authorized_grant_types` SET('AUTHORIZATION_CODE','CLIENT_CREDENTIALS') NOT NULL ,
  `organization_id` INT NOT NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `fk_client_organization1` (`organization_id` ASC) ,
  UNIQUE INDEX `client_id_UNIQUE` (`client_id` ASC) ,
  CONSTRAINT `fk_client_organization1`
    FOREIGN KEY (`organization_id` )
    REFERENCES `contrail_oauth_as`.`organization` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`owner`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`owner` (
  `id` INT(11) NOT NULL AUTO_INCREMENT ,
  `uuid` VARCHAR(255) NOT NULL ,
  `country_restriction` TINYINT(1)  NOT NULL DEFAULT 0 ,
  `owner_type` ENUM('USER','SERVICE') NOT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `uuid_UNIQUE` (`uuid` ASC) )
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`access_token`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`access_token` (
  `id` INT(11) NOT NULL AUTO_INCREMENT ,
  `token` VARCHAR(256) NOT NULL ,
  `expire_time` DATETIME NOT NULL ,
  `client_id` INT(11) NOT NULL ,
  `owner_id` INT(11) NOT NULL ,
  `scope` VARCHAR(1000) NULL ,
  `revoked` TINYINT(1)  NULL DEFAULT 0 ,
  PRIMARY KEY (`id`) ,
  INDEX `FK_access_token` (`client_id` ASC) ,
  INDEX `fk_access_token_user` (`owner_id` ASC) ,
  UNIQUE INDEX `token_UNIQUE` (`token` ASC) ,
  CONSTRAINT `FK_access_token`
    FOREIGN KEY (`client_id` )
    REFERENCES `contrail_oauth_as`.`client` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_access_token_user`
    FOREIGN KEY (`owner_id` )
    REFERENCES `contrail_oauth_as`.`owner` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`authz_code`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`authz_code` (
  `id` INT(11) NOT NULL AUTO_INCREMENT ,
  `code` VARCHAR(256) NOT NULL ,
  `redirect_uri` VARCHAR(256) NOT NULL ,
  `expire_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  `client_id` INT(11) NOT NULL ,
  `scope` VARCHAR(1000) NULL DEFAULT NULL ,
  `owner_id` INT(11) NOT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `code` (`code` ASC) ,
  INDEX `FK_authz_code` (`client_id` ASC) ,
  INDEX `fk_authz_code_user` (`owner_id` ASC) ,
  CONSTRAINT `FK_authz_code`
    FOREIGN KEY (`client_id` )
    REFERENCES `contrail_oauth_as`.`client` (`id` ),
  CONSTRAINT `fk_authz_code_user`
    FOREIGN KEY (`owner_id` )
    REFERENCES `contrail_oauth_as`.`owner` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`token_info_access_log`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`token_info_access_log` (
  `id` INT(11) NOT NULL AUTO_INCREMENT ,
  `access_token_id` INT(11) NOT NULL ,
  `bearer_name` VARCHAR(255) NOT NULL ,
  `resource_server_name` VARCHAR(255) NOT NULL ,
  `timestamp` TIMESTAMP NOT NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `fk_access_log_access_token1` (`access_token_id` ASC) ,
  CONSTRAINT `fk_access_log_access_token1`
    FOREIGN KEY (`access_token_id` )
    REFERENCES `contrail_oauth_as`.`access_token` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`country`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`country` (
  `code` VARCHAR(2) NOT NULL ,
  `name` VARCHAR(50) NOT NULL ,
  PRIMARY KEY (`code`) )
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`client_has_country`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`client_has_country` (
  `country_code` VARCHAR(2) NOT NULL ,
  `client_id` INT(11) NOT NULL ,
  PRIMARY KEY (`country_code`, `client_id`) ,
  INDEX `fk_client_has_country_client1` (`client_id` ASC) ,
  INDEX `fk_client_has_country_country1` (`country_code` ASC) ,
  CONSTRAINT `fk_client_has_country_country1`
    FOREIGN KEY (`country_code` )
    REFERENCES `contrail_oauth_as`.`country` (`code` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_client_has_country_client1`
    FOREIGN KEY (`client_id` )
    REFERENCES `contrail_oauth_as`.`client` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`client_trust`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`client_trust` (
  `owner_id` INT(11) NOT NULL ,
  `client_id` INT(11) NOT NULL ,
  `trust_level` ENUM('TRUSTED','NOT_TRUSTED') NOT NULL ,
  PRIMARY KEY (`owner_id`, `client_id`) ,
  INDEX `fk_owner_has_client_client1` (`client_id` ASC) ,
  INDEX `fk_owner_has_client_owner1` (`owner_id` ASC) ,
  CONSTRAINT `fk_owner_has_client_owner1`
    FOREIGN KEY (`owner_id` )
    REFERENCES `contrail_oauth_as`.`owner` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_owner_has_client_client1`
    FOREIGN KEY (`client_id` )
    REFERENCES `contrail_oauth_as`.`client` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`organization_trust`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`organization_trust` (
  `owner_id` INT(11) NOT NULL ,
  `organization_id` INT NOT NULL ,
  `trust_level` ENUM('FULLY','PARTLY','DENIED') NOT NULL ,
  PRIMARY KEY (`owner_id`, `organization_id`) ,
  INDEX `fk_owner_has_organization_organization1` (`organization_id` ASC) ,
  INDEX `fk_owner_has_organization_owner1` (`owner_id` ASC) ,
  CONSTRAINT `fk_owner_has_organization_owner1`
    FOREIGN KEY (`owner_id` )
    REFERENCES `contrail_oauth_as`.`owner` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_owner_has_organization_organization1`
    FOREIGN KEY (`organization_id` )
    REFERENCES `contrail_oauth_as`.`organization` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB;


-- -----------------------------------------------------
-- Table `contrail_oauth_as`.`country_trust`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `contrail_oauth_as`.`country_trust` (
  `owner_id` INT(11) NOT NULL ,
  `country_code` VARCHAR(2) NOT NULL ,
  `is_trusted` TINYINT(1)  NULL ,
  PRIMARY KEY (`owner_id`, `country_code`) ,
  INDEX `fk_owner_has_country_country1` (`country_code` ASC) ,
  INDEX `fk_owner_has_country_owner1` (`owner_id` ASC) ,
  CONSTRAINT `fk_owner_has_country_owner1`
    FOREIGN KEY (`owner_id` )
    REFERENCES `contrail_oauth_as`.`owner` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_owner_has_country_country1`
    FOREIGN KEY (`country_code` )
    REFERENCES `contrail_oauth_as`.`country` (`code` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = INNODB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
