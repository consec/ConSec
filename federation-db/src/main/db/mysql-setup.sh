#!/bin/bash

# exit on error
set -e

SCHEMA_PATH="/usr/share/contrail/federation-db/Providers-db.sql"
INITIAL_DATA="/usr/share/contrail/federation-db/initial-data"
MYSQL=`which mysql`
Q3="GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON contrail.* TO 'contrail'@'localhost' IDENTIFIED BY 'contrail';"
Q3_NDB="GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON contrail.* TO 'contrail'@'%' IDENTIFIED BY 'contrail';"
SQL="${Q3}"

if [ "$CONTRAIL_FEDERATION_DB_USE_MYSQL_CLUSTER" == "1" ]
then
	cp -f $SCHEMA_PATH ${SCHEMA_PATH}-ndb
        sed -i 's/InnoDB/NDBCLUSTER/g' ${SCHEMA_PATH}-ndb
	SCHEMA_PATH="${SCHEMA_PATH}-ndb"
fi

if [ "$DEBIAN_FRONTEND" == "noninteractive" ]
then
	mysqlname="root"
	mysqlpasswd=""
else
	read -p "Please, provide mysql user name with admin rights: " mysqlname
	read -s -p "Please, provide mysql user's (${mysqlname}) password: " mysqlpasswd
fi
$MYSQL -u${mysqlname} --password="${mysqlpasswd}"  < ${SCHEMA_PATH}
$MYSQL -u${mysqlname} --password="${mysqlpasswd}" contrail  < ${INITIAL_DATA}
$MYSQL -u${mysqlname} --password="${mysqlpasswd}" -e "$SQL"

if [ "$CONTRAIL_FEDERATION_DB_USE_MYSQL_CLUSTER" == "1" ]
then
        $MYSQL -u${mysqlname} --password="${mysqlpasswd}" -e "$Q3_NDB"
fi
