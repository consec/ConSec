SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `consec` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `consec` ;

-- -----------------------------------------------------
-- Table `user`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `user` (
  `user_id` VARCHAR(36) NOT NULL ,
  `username` VARCHAR(45) NOT NULL ,
  `attributes` TEXT NULL ,
  `first_name` VARCHAR(255) NOT NULL ,
  `last_name` VARCHAR(255) NOT NULL ,
  `email` VARCHAR(255) NOT NULL ,
  `password` VARCHAR(255) NOT NULL ,
  PRIMARY KEY (`user_id`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `role`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `role` (
  `role_id` INT NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(45) NULL ,
  `description` VARCHAR(255) NULL ,
  `acl` TEXT NULL ,
  PRIMARY KEY (`role_id`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `group`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `group` (
  `group_id` INT NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(45) NULL ,
  `description` VARCHAR(255) NULL ,
  PRIMARY KEY (`group_id`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `attribute`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `attribute` (
  `attribute_id` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `uri` VARCHAR(255) NULL ,
  `description` VARCHAR(255) NULL ,
  `default_value` VARCHAR(255) NULL ,
  `reference` VARCHAR(45) NULL ,
  PRIMARY KEY (`attribute_id`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `user_has_attribute`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `user_has_attribute` (
  `user_id` VARCHAR(36) NOT NULL ,
  `attribute_id` VARCHAR(36) NOT NULL ,
  `value` VARCHAR(255) NULL ,
  `referenceId` INT NULL ,
  PRIMARY KEY (`user_id`, `attribute_id`) ,
  INDEX `fk_User_has_Attribute_Attribute1_idx` (`attribute_id` ASC) ,
  INDEX `fk_User_has_Attribute_User1_idx` (`user_id` ASC) ,
  CONSTRAINT `fk_User_has_Attribute_User1`
    FOREIGN KEY (`user_id` )
    REFERENCES `user` (`user_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_User_has_Attribute_Attribute1`
    FOREIGN KEY (`attribute_id` )
    REFERENCES `attribute` (`attribute_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `identity_provider`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `identity_provider` (
  `idp_id` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(255) NULL ,
  `uri` VARCHAR(255) NOT NULL ,
  `description` VARCHAR(255) NULL ,
  `attributes` TEXT NULL ,
  PRIMARY KEY (`idp_id`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `user_has_identity_provider`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `user_has_identity_provider` (
  `user_id` VARCHAR(36) NOT NULL ,
  `idp_id` VARCHAR(36) NOT NULL ,
  `identity` VARCHAR(255) NOT NULL ,
  `attributes` TEXT NULL ,
  PRIMARY KEY (`user_id`, `idp_id`) ,
  INDEX `fk_User_has_identityProvider_identityProvider1_idx` (`idp_id` ASC) ,
  INDEX `fk_User_has_identityProvider_User1_idx` (`user_id` ASC) ,
  CONSTRAINT `fk_User_has_identityProvider_User1`
    FOREIGN KEY (`user_id` )
    REFERENCES `user` (`user_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_User_has_identityProvider_identityProvider1`
    FOREIGN KEY (`idp_id` )
    REFERENCES `identity_provider` (`idp_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `user_has_role`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `user_has_role` (
  `user_id` VARCHAR(36) NOT NULL ,
  `role_id` INT NOT NULL ,
  PRIMARY KEY (`user_id`, `role_id`) ,
  INDEX `fk_User_has_Role_Role1_idx` (`role_id` ASC) ,
  INDEX `fk_User_has_Role_User1_idx` (`user_id` ASC) ,
  CONSTRAINT `fk_User_has_Role_User1`
    FOREIGN KEY (`user_id` )
    REFERENCES `user` (`user_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_User_has_Role_Role1`
    FOREIGN KEY (`role_id` )
    REFERENCES `role` (`role_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `user_has_group`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `user_has_group` (
  `user_id` VARCHAR(36) NOT NULL ,
  `group_id` INT NOT NULL ,
  PRIMARY KEY (`user_id`, `group_id`) ,
  INDEX `fk_User_has_Group_Group1_idx` (`group_id` ASC) ,
  INDEX `fk_User_has_Group_User1_idx` (`user_id` ASC) ,
  CONSTRAINT `fk_User_has_Group_User1`
    FOREIGN KEY (`user_id` )
    REFERENCES `user` (`user_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_User_has_Group_Group1`
    FOREIGN KEY (`group_id` )
    REFERENCES `group` (`group_id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
