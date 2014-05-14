#!/bin/bash

S_ROOT="/usr/share/simplesamlphp-1.9.0"
C_PWD=`pwd`

#1)SimpleSAMLphp installation:
cd /usr/share
wget http://simplesamlphp.googlecode.com/files/simplesamlphp-1.9.0.tar.gz
gunzip < simplesamlphp-1.9.0.tar.gz | tar xvf -
rm simplesamlphp-1.9.0.tar.gz

#2)Dependencies
apt-get install apache2 libapache2-mod-php5 openssl php-openid php-xml-parser php5 php5-mcrypt php5-mhash zlib1g php5-mysql php5-radius php5-curl php5-suhosin

#3)Set-up SimpleSAMLphp
cd ${C_PWD}

cp -r usr/share/simplesamlphp-1.9.0/* ${S_ROOT}/
cp -r openssl-gen-cert ${S_ROOT}/
cp -r contrail-openssl.cnf ${S_ROOT}/
cp -r var/lib/simplesamlphp-openid-provider /var/lib/
cp -r etc/apache2/sites-available/* /etc/apache2/sites-available/

#username in $S_ROOT/config/authsources.php is set to contrail
sed -i "s/'username' => 'root'/'username' => 'contrail'/" $S_ROOT/config/authsources.php #replaces username also in section example-userpass. Is that ok ???

#$S_ROOT/config/config.php is unchanged (except salt)
salt=$(tr -c -d '0123456789abcdefghijklmnopqrstuvwxyz' </dev/urandom | dd bs=32 count=1 2>/dev/null)
sed -i "s/'secretsalt' => 'als235xowqp321flae84310x139fx140fjoiwx1049uxfh591'/'secretsalt' => '$salt'/" $S_ROOT/config/config.php

chown -R www-data:www-data ${S_ROOT}/log

#Copy metadata/saml20-sp-remote.php under $S_ROOT. You will need to change metadata of 'http://localhost:8001/saml2/metadata/' according to the SP (e.g. contrail-federation-web).

touch ${S_ROOT}/modules/openid/enable
touch ${S_ROOT}/modules/openidProvider/enable
touch ${S_ROOT}/modules/saml2debug/enable

#1. Edit /etc/php5/apache2/php.ini and change allow_call_time_pass_reference variable from Off to On.
sed -i 's/allow_call_time_pass_reference = Off/allow_call_time_pass_reference = On/' /etc/php5/apache2/php.ini
#2.Under /etc/php5/conf.d/suhosin.ini change the value of this variable from 512 to 2048:
sed -i 's/;suhosin.get.max_value_length = 512/suhosin.get.max_value_length = 2048/' /etc/php5/conf.d/suhosin.ini

#4) Create server certificates
cd ${C_PWD}
./openssl-gen-cert
cp -r cert/contrail-federation-idp.cert $S_ROOT/cert
cp -r cert/contrail-federation-idp.key $S_ROOT/cert

#5) Apache2 set-up
cp -r etc/apache2/sites-available/simplesaml* /etc/apache2/sites-available/

#Paths in /etc/apache2/sites-available/simplesaml-ssl of SSL cert and key are default

cd /etc/apache2/sites-available
a2dissite default
a2ensite simplesaml-ssl
a2ensite simplesaml
a2enmod ssl
service apache2 restart

printf "\n***\nimport the certificate (/usr/share/simplesamlphp-1.9.0/cert/contrail-federation-idp.cert) of the server to your browser.\n***\n"
