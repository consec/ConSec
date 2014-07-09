SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0;
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'TRADITIONAL';


-- -----------------------------------------------------
-- Table `audit_event`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_event` (
  `audit_event_id` INT          NOT NULL AUTO_INCREMENT,
  `eventType`      VARCHAR(45)  NULL,
  `initiatorId`    VARCHAR(255) NULL,
  `initiatorType`  VARCHAR(45)  NULL,
  `action`         VARCHAR(45)  NULL,
  `eventTime`      TIMESTAMP    NULL,
  `targetId`       VARCHAR(255) NULL,
  `targetType`     VARCHAR(45)  NULL,
  `outcome`        VARCHAR(45)  NULL,
  `severity`       VARCHAR(45)  NULL,
  PRIMARY KEY (`audit_event_id`))
  ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `attachment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `attachment` (
  `attachment_id`  INT         NOT NULL AUTO_INCREMENT,
  `audit_event_id` INT         NOT NULL,
  `name`           VARCHAR(45) NOT NULL,
  `contentType`           VARCHAR(45) NOT NULL,
  `content`        TEXT        NOT NULL,
  PRIMARY KEY (`attachment_id`),
  INDEX `fk_attachment_audit_event` (`audit_event_id` ASC),
  CONSTRAINT `fk_attachment_audit_event`
  FOREIGN KEY (`audit_event_id`)
  REFERENCES `audit_event` (`audit_event_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
  ENGINE = InnoDB;


SET SQL_MODE = @OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS;
