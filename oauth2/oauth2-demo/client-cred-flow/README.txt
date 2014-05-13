*******************************************************************************
                       OAuth Client Credentials Flow Demo
*******************************************************************************

I. Prerequsities

* oauth-as
* ca-server (optionally, for obtaining user certificates)

II. Installation

Download package contrail-oauth2-client-cred-flow-demo.tar.gz or build from sources:
mvn clean package

Package structure:

/etc/contrail/oauth2-client-cred-flow-demo/
    * oauth2-client-cred-flow-demo.properties
/usr/share/contrail/oauth2-client-cred-flow-demo/
    * log4j.properties
    * lib/
        * (jar files)

Extract the package to root directory (/).

III. Configuration

Configuration files:
* /etc/contrail/oauth2-client-cred-flow-demo/oauth2-client-cred-flow-demo.properties
* /usr/share/contrail/oauth2-client-cred-flow-demo/log4j.properties

IV. Running the application

To obtain an access token from the OAuth authorization server for the specified user:
./oauth2-client-cred-flow-demo.sh getToken <userUUID>

To obtain a delegated user certificate from the CA server on behalf of the user using the
specified access token:
./oauth2-client-cred-flow-demo.sh getCert <access_token>
