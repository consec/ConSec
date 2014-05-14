#!/bin/bash

S_ROOT="/usr/share/simplesamlphp-1.9.0"
CONFIG_DIR="/etc/contrail/contrail-federation-id-prov-support/"

#1)SimpleSAMLphp installation:
mkdir -p ${S_ROOT}
cp -r ../php/* ${S_ROOT}/

#2)Dependencies -- are in the debian.control file

#apt-get install apache2 libapache2-mod-php5 openssl php-openid php-xml-parser php5 php5-mcrypt php5-mhash zlib1g php5-mysql php5-radius php5-curl php5-suhosin

#3)Set-up SimpleSAMLphp
cp -r usr/share/simplesamlphp-1.9.0/* ${S_ROOT}/
mkdir -p ${CONFIG_DIR}
cp -s ${S_ROOT}/config/* ${CONFIG_DIR}/
cp -r openssl-gen-cert ${CONFIG_DIR}/
cp -r contrail-openssl.cnf ${CONFIG_DIR}/
cp -r var/lib/simplesamlphp-openid-provider /var/lib/
cp -r etc/apache2/sites-available/* /etc/apache2/sites-available/

#username in $S_ROOT/config/authsources.php is set to contrail
sed -i "s/'username' => 'root'/'username' => 'contrail'/" $S_ROOT/config/authsources.php #replaces username also in section example-userpass. Is that ok ???

#$S_ROOT/config/config.php is unchanged (except salt)
salt=$(tr -c -d '0123456789abcdefghijklmnopqrstuvwxyz' </dev/urandom | dd bs=32 count=1 2>/dev/null)
sed -i "s/'secretsalt' => 'als235xowqp321flae84310x139fx140fjoiwx1049uxfh591'/'secretsalt' => '$salt'/" $S_ROOT/config/config.php

chown -R www-data:www-data ${S_ROOT}/log
chown -R www-data:www-data ${S_ROOT}/www

#Copy metadata/saml20-sp-remote.php under $S_ROOT. You will need to change metadata of 'http://localhost:8001/saml2/metadata/' according to the SP (e.g. contrail-federation-web).

touch ${S_ROOT}/modules/openid/enable
touch ${S_ROOT}/modules/openidProvider/enable
touch ${S_ROOT}/modules/saml2debug/enable

#1. Edit /etc/php5/apache2/php.ini and change allow_call_time_pass_reference variable from Off to On.
sed -i 's/allow_call_time_pass_reference = Off/allow_call_time_pass_reference = On/' /etc/php5/apache2/php.ini
#2.Under /etc/php5/conf.d/suhosin.ini change the value of this variable from 512 to 2048:
sed -i 's/;suhosin.get.max_value_length = 512/suhosin.get.max_value_length = 2048/' /etc/php5/conf.d/suhosin.ini

#4) Create server certificates
# ./openssl-gen-cert
# cp -r cert/contrail-federation-idp.cert $S_ROOT/cert
# cp -r cert/contrail-federation-idp.key $S_ROOT/cert

#5) Apache2 set-up
cp -r etc/apache2/sites-available/* /etc/apache2/sites-available/
cat etc/apache2/ports-to-add.conf >> /etc/apache2/ports.conf

#Paths in /etc/apache2/sites-available/simplesaml-ssl of SSL cert and key are default

cd /etc/apache2/sites-available
a2dissite default
a2ensite wildcard-simplesaml-ssl
a2ensite simplesaml
a2enmod ssl
service apache2 restart

printf "\n***\nAdding contrail-federation-web and providers entries into /etc/hosts file.\n***\n"

echo "127.0.0.1 contrail-idp.contrail.eu" >> /etc/hosts
echo "127.0.0.1 multi.contrail-idp.contrail.eu" >> /etc/hosts

printf "\n***\nConsider modifying /etc/hosts if needed. If DNS server resolves hosts in metadata configuration, remove entries in /etc/hosts.\n***\n"

printf "\n***\nOn the client machine, you need to resolve: \n"
printf "\n * multi.contrail-idp.contrail.eu\n"
printf "\nMaybe you will need to modify your /etc/hosts file.\n***\n"

printf "\n***\nimport the certificate (/usr/share/simplesamlphp-1.9.0/cert/contrail-federation-idp.cert) of the server to your browser.\n***\n"
