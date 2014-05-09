#!/bin/bash

################################################################################
#       Script inserting example values into existing database via REST
#
#       For any questions/comments please send me an email (ales.cernivec@xlab.si). 
################################################################################
EXPECTED_ARGS=2
E_BADARGS=65
if [ $# -ne $EXPECTED_ARGS ]
then
  echo "Usage: `basename $0` [REST server hostname] [REST server port]"
  exit $E_BADARGS
fi

HOST=${1}
PORT=${2}
HP="http://${HOST}:${PORT}/federation-api/"

JOHNPWD=`echo johnspwd | shasum`
BOBPWD=`echo bobspwd | shasum`
ALICEPWD=`echo alicespwd | shasum`

echo "Inserting attributes into ${HP}"
curl -X POST --header "Content-Type: application/json" -d '{"description":"Contrail users telephones.","name":"phone","attributeId":1,"defaultValue":"000000","uri":"contrail:phone","reference":"/users"}' ${HP}attributes
curl -X POST --header "Content-Type: application/json" -d '{"description":"Contrail users reputation.","name":"reputation","attributeId":2,"defaultValue":"good","uri":"contrail:reputation","reference":"/users"}' ${HP}attributes
curl -X POST --header "Content-Type: application/json" -d '{"description":"Registration date","name":"reg-date","defaultValue":"0","uri":"contrail:reg-date","reference":"/users"}' ${HP}attributes

echo "Inserting user roles into ${HP}"
curl -X POST --header "Content-Type: application/json" -d '{name: FederationUser , description: "This is normal federation user.", acl: ""}' ${HP}roles
curl -X POST --header "Content-Type: application/json" -d '{name: FederationCoordinator , description: "Role for federation coordinator.", acl: ""}' ${HP}roles
curl -X POST --header "Content-Type: application/json" -d '{name: CloudAdministrator , description: "Role for Cloud Administrator.", acl: ""}' ${HP}roles

echo "Inserting user groups into ${HP}"
curl -X POST --header "Content-Type: application/json" -d '{name: "developer" , description: "Developers"}' ${HP}groups
curl -X POST --header "Content-Type: application/json" -d '{name: "user" , description: "Role for federation coordinator.", acl: ""}' ${HP}groups

echo "Inserting users into ${HP}"
curl -X POST --header "Content-Type: application/json" -d "{username: john , firstName: John, lastName: Jonatan, email:joh@contrail.eu, password: ${JOHNPWD} }" ${HP}users
curl -X POST --header "Content-Type: application/json" -d "{username: alice,firstName: Alice,lastName: Although, email:alice@contrail.eu, password: ${ALICEPWD} }" ${HP}users
curl -X POST --header "Content-Type: application/json" -d "{username: bob,firstName: Bob,lastName: Sinclare, email:bob@contrail.eu, password: ${BOBPWD} }" ${HP}users

echo "Adding users role ${HP} to a user"
curl -X POST --header "Content-Type: application/json" -d "{roleId: 1}" ${HP}users/1/roles

echo "Inserting user attributes into ${HP}"
echo "John\n"
curl -X POST --header "Content-Type: application/json" -d '{value: "14-feb-2012" , "referenceId": "1", attributeId:"3"}' ${HP}users/1/attributes
curl -X POST --header "Content-Type: application/json" -d '{value: good , "referenceId": "1", attributeId:"2"}' ${HP}users/1/attributes
curl -X POST --header "Content-Type: application/json" -d '{value: "040214241" , referenceId: "1", attributeId:"1"}' ${HP}users/1/attributes
echo "Alice\n"
curl -X POST --header "Content-Type: application/json" -d '{value: "1-1-2012" , referenceId: 2, attributeId:3}' ${HP}users/2/attributes
curl -X POST --header "Content-Type: application/json" -d "{value: bad , referenceId: 2, attributeId:2}" ${HP}users/2/attributes
curl -X POST --header "Content-Type: application/json" -d '{value: "+123456" , referenceId: 2, attributeId:1}' ${HP}users/2/attributes
echo "Bob\n"
curl -X POST --header "Content-Type: application/json" -d '{value: "1-1-2111" , referenceId: 3, attributeId:3}' ${HP}users/3/attributes
curl -X POST --header "Content-Type: application/json" -d "{value: newgroup , referenceId: 3, attributeId:2}" ${HP}users/3/attributes
curl -X POST --header "Content-Type: application/json" -d '{value: "040214241" , referenceId: 3, attributeId:1}' ${HP}users/3/attributes

echo "Inserting user applications into ${HP}"
curl -X POST --header "Content-Type: application/json" -d '{deploymentDesc: "",name: "newApplication",applicationOvf: "", attributes:""}' ${HP}users/1/applications
curl -X POST --header "Content-Type: application/json" -d '{deploymentDesc: "",name: "phpServer",applicationOvf: "", attributes:""}' ${HP}users/1/applications
