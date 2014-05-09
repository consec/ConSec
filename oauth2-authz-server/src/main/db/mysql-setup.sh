#!/bin/bash

# exit on error
set -e

mkdir -p /var/log/contrail/
chown tomcat6:tomcat6 /var/log/contrail/

SCHEMA_PATH="/usr/share/contrail/contrail-oauth-as/contrail-oauth-as-db-schema.sql"
INITIAL_DATA="/usr/share/contrail/contrail-oauth-as/contrail-oauth-as-initial-data.sql"
MYSQL=`which mysql`
Q3="GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON contrail_oauth_as.* TO 'contrail'@'localhost' IDENTIFIED BY 'contrail';"
Q3_NDB="GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON contrail_oauth_as.* TO 'contrail'@'%' IDENTIFIED BY 'contrail';"
SQL="${Q3}"

if [ "$DEBIAN_FRONTEND" == "noninteractive" ]
then
	mysqlname="root"
	mysqlpasswd=""
else
	read -p "Please, provide mysql user name with admin rights: " mysqlname
	read -s -p "Please, provide mysql user's (${mysqlname}) password: " mysqlpasswd
fi
$MYSQL -u${mysqlname} --password="${mysqlpasswd}"  < ${SCHEMA_PATH}
$MYSQL -u${mysqlname} --password="${mysqlpasswd}" contrail_oauth_as  < ${INITIAL_DATA}
$MYSQL -u${mysqlname} --password="${mysqlpasswd}" -e "$SQL"
