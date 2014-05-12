/*Data for the table `country` */

insert  into `country`(`code`,`name`) values ('DE','Germany');
insert  into `country`(`code`,`name`) values ('FR','France');
insert  into `country`(`code`,`name`) values ('GB','United Kingdom');
insert  into `country`(`code`,`name`) values ('IT','Italy');
insert  into `country`(`code`,`name`) values ('NL','Netherlands');
insert  into `country`(`code`,`name`) values ('SI','Slovenia');

/*Data for the table `organization` */

insert  into `organization`(`id`,`name`) values (1,'XLAB');
insert  into `organization`(`id`,`name`) values (6,'FEDERATION');

/*Data for the table `client` */

insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (1,'oauth-java-client-demo','oauth-java-client-demo','https://contrail.xlab.si:8444/oauth-demo/oauth2callback','somesecret','AUTHORIZATION_CODE,CLIENT_CREDENTIALS',1);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (5,'oauth-python-client-demo','OAuth Python Client Demo','https://portal.contrail.xlab.si/oauth2callback','somesecret','AUTHORIZATION_CODE,CLIENT_CREDENTIALS',1);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (7,'oauth-cc-demo-client','oauth-cc-demo-client',NULL,'somesecret','CLIENT_CREDENTIALS',1);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (8,'auditing-demo','auditing-demo',NULL,'somesecret','CLIENT_CREDENTIALS',1);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (9,'federation-accounting','federation_accounting',NULL,'somesecret','CLIENT_CREDENTIALS',6);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (10,'federation-web','federation-web','https://portal.contrail.xlab.si/oauth2callback','somesecret','AUTHORIZATION_CODE,CLIENT_CREDENTIALS',1);
insert  into `client`(`id`,`client_id`,`name`,`callback_uri`,`client_secret`,`authorized_grant_types`,`organization_id`) values (11,'audit-manager','audit-manager',NULL,'somesecret','CLIENT_CREDENTIALS',6);

/*Data for the table `owner` */

insert  into `owner`(`id`,`uuid`,`country_restriction`,`owner_type`) values (1,'caa6e102-8ff0-400f-a120-23149326a936',0,'USER');
insert  into `owner`(`id`,`uuid`,`country_restriction`,`owner_type`) values (12,'FEDERATION',0,'SERVICE');

/*Data for the table `client_has_country` */

insert  into `client_has_country`(`country_code`,`client_id`) values ('SI',1);
insert  into `client_has_country`(`country_code`,`client_id`) values ('SI',5);
insert  into `client_has_country`(`country_code`,`client_id`) values ('SI',7);
insert  into `client_has_country`(`country_code`,`client_id`) values ('SI',8);
insert  into `client_has_country`(`country_code`,`client_id`) values ('SI',10);

/*Data for the table `client_trust` */

/*Data for the table `organization_trust` */

insert  into `organization_trust`(`owner_id`,`organization_id`,`trust_level`) values (1,1,'FULLY');
insert  into `organization_trust`(`owner_id`,`organization_id`,`trust_level`) values (1,6,'FULLY');
insert  into `organization_trust`(`owner_id`,`organization_id`,`trust_level`) values (12,6,'FULLY');

/*Data for the table `country_trust` */
