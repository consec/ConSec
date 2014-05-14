Note: THIS SHOULD BE USED ONLY FOR TESTING PURPOSES!
author: ales.cernivec@xlab.si

Create new CA (self signed):
openssl req -newkey rsa:2048 -nodes -keyform PEM -keyout ca.key -x509 -days 3650 -outform PEM -out ca.cer

Generate new host certificate (e.g. for contrail-federation-web) with serial number 100:
openssl genrsa -out contrail-federation-web.key 2048
openssl req -new -key contrail-federation-web.key -out contrail-federation-web.req
openssl x509 -req -in contrail-federation-web.req -CA ca.cer -CAkey ca.key -set_serial 100 -days 3650 -outform PEM -out contrail-federation-web.cert