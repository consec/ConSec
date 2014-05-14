Contrail Federation Identity Provider Support package
-----------------------------------------------------

Author: ales.cernivec@xlab.si

Licence:
	* BSD (see LICENCE.txt)
	* This package includes SimpleSAMLphp v 1.9.0 (licenced by LGPL v2.1 - see COPYING file).

Supported OS:
	* Debian 6.0, 
	* Ubuntu 11.10, 12.04 LTS

Dependencies: 
	All dependencies are installed with src/main/resources/install-script.sh in the installation step.
	* apache2 
	* libapache2-mod-php5 
	* openssl 
	* php-openid 
	* php-xml-parser 
	* php5 
	* php5-mcrypt 
	* php5-mhash 
	* zlib1g 
	* php5-mysql 
	* php5-radius 
	* php5-curl 
	* php5-suhosin

Installation:
	$ cd  src/main/resources/
	$ sudo ./install-script.sh

Check the installation:
	Navigate to http://localhost/simplesamlphp with your favourite browser. You should see SimpleSAMLphp's welcome page.
