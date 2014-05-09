Note: THIS SHOULD BE USED ONLY FOR TESTING PURPOSES!
author: ales.cernivec@xlab.si

Note: step 1 is not necessary since CA's keypair is already provided.

1) Create new CA (self signed):
openssl req -newkey rsa:2048 -nodes -keyform PEM -keyout ca.key -x509 -days 3650 -outform PEM -out ca.crt

2)Generate new host certificate (e.g. for contrail-federation-api) with serial number 100:
openssl genrsa -out contrail-federation-api.key 2048
openssl req -new -key contrail-federation-api.key -out contrail-federation-api.req
openssl x509 -req -in contrail-federation-api.req -CA ca.crt -CAkey ca.key -set_serial 100 -days 3650 -outform PEM -out contrail-federation-api.crt

3) openssl pkcs12 -export -in contrail-federation-api.crt -inkey contrail-federation-api.key -CAfile ca.crt -out contrail-federation-api.p12 -chain

Use password "contrail"

4) # chown tomcat7.tomcat7 contrail-federation-api.p12

5) keytool -import -file ca.crt -alias contrailCA -keystore contrailTrustStore

Use password "contrail"
