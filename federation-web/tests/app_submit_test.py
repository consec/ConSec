import re
import json
import requests

fedApiAddress = "http://localhost:8080/federation-api"
userUuid = "caa6e102-8ff0-400f-a120-23149326a936"

# register user SLAT
data = {
    "name":"Test SLAT",
    "url":"http://contrail.xlab.si/test-files/ConpaasSLATSmall.xml",
    "content":"<ns0:SLATemplate xmlns:ns0=\"http://www.slaatsoi.eu/slamodel\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.slaatsoi.eu/slamodel file:///C:/SLA@SOI/slasoi.xsd\"><ns0:Text /><ns0:Properties></ns0:Properties><ns0:UUID>Contrail-SLAT-NewFeatures-02</ns0:UUID><ns0:ModelVersion>sla_at_soi_sla_model_v1.0</ns0:ModelVersion><ns0:Party><ns0:Text /><ns0:Properties><ns0:Entry><ns0:Key>http://www.slaatsoi.org/slamodel#gslam_epr</ns0:Key><ns0:Value>http://146.48.81.249:8080/services/federationNegotiation?wsdl</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>Contrailfederation</ns0:ID><ns0:Role>http://www.slaatsoi.org/slamodel#provider</ns0:Role></ns0:Party><ns0:Party><ns0:Text /><ns0:Properties><ns0:Entry><ns0:Key>http://www.slaatsoi.org/slamodel#gslam_epr</ns0:Key><ns0:Value>http://customerEndpoint.contrail.org:8080/services/ISNegotiation?wsdl</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>333</ns0:ID><ns0:Role>http://www.slaatsoi.org/slamodel#customer</ns0:Role></ns0:Party><ns0:InterfaceDeclr><ns0:Text>Interface to specific OVF item</ns0:Text><ns0:Properties><ns0:Entry><ns0:Key>OVF_URL</ns0:Key><ns0:Value>http://146.48.81.249:8085/ovfs/ConpaasOvfSmall.ovf</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>OVF-Item-VirtualSystem1</ns0:ID><ns0:ProviderRef>ContrailProvider</ns0:ProviderRef><ns0:Endpoint><ns0:Text /><ns0:Properties><ns0:Entry><ns0:Key>OVF_VirtualSystem_ID</ns0:Key><ns0:Value>VirtualSystem1</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>VirtualSystem1-VM-Type</ns0:ID><ns0:Location>VEP-ID</ns0:Location><ns0:Protocol>http://www.slaatsoi.org/slamodel#HTTP</ns0:Protocol></ns0:Endpoint><ns0:Interface><ns0:InterfaceResourceType><ns0:Text /><ns0:Properties /><ns0:Name>OVFAppliance</ns0:Name></ns0:InterfaceResourceType></ns0:Interface></ns0:InterfaceDeclr><ns0:InterfaceDeclr><ns0:Text>Interface to specific OVF item</ns0:Text><ns0:Properties><ns0:Entry><ns0:Key>OVF_URL</ns0:Key><ns0:Value>http://146.48.81.249:8085/ovfs/ConpaasOvfSmall.ovf</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>OVF-Item-VirtualSystem2</ns0:ID><ns0:ProviderRef>ContrailProvider</ns0:ProviderRef><ns0:Endpoint><ns0:Text /><ns0:Properties><ns0:Entry><ns0:Key>OVF_VirtualSystem_ID</ns0:Key><ns0:Value>VirtualSystem2</ns0:Value></ns0:Entry></ns0:Properties><ns0:ID>VirtualSystem2-VM-Type</ns0:ID><ns0:Location>VEP-ID</ns0:Location><ns0:Protocol>http://www.slaatsoi.org/slamodel#HTTP</ns0:Protocol></ns0:Endpoint><ns0:Interface><ns0:InterfaceResourceType><ns0:Text /><ns0:Properties /><ns0:Name>OVFAppliance</ns0:Name></ns0:InterfaceResourceType></ns0:Interface></ns0:InterfaceDeclr><ns0:AgreementTerm><ns0:Text /><ns0:Properties /><ns0:ID>VirtualSystem2_Guarantees</ns0:ID><ns0:VariableDeclr><ns0:Text /><ns0:Properties /><ns0:Var>VM_of_type_VirtualSystem2</ns0:Var><ns0:Expr><ns0:ValueExpr><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/coremodel#subset_of</ns0:Operator><ns0:Parameter><ns0:ID>OVF-Item-VirtualSystem2</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:ValueExpr></ns0:Expr></ns0:VariableDeclr><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>CPU_SPEED_for_VirtualSystem2</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#cpu_speed</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem2</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>1800</ns0:Value><ns0:Datatype>http://www.slaatsoi.org/coremodel/units#MHz</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>CPU_CORES_for_VirtualSystem2</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#vm_cores</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem2</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>2</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>MEMORY_for_VirtualSystem2</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#memory</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem2</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>16384</ns0:Value><ns0:Datatype>http://www.slaatsoi.org/coremodel/units#MB</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>Reservation_for_VirtualSystem2</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#reserve</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem2</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>1</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed></ns0:AgreementTerm><ns0:AgreementTerm><ns0:Text /><ns0:Properties /><ns0:ID>VirtualSystem1_Guarantees</ns0:ID><ns0:VariableDeclr><ns0:Text /><ns0:Properties /><ns0:Var>VM_of_type_VirtualSystem1</ns0:Var><ns0:Expr><ns0:ValueExpr><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/coremodel#subset_of</ns0:Operator><ns0:Parameter><ns0:ID>OVF-Item-VirtualSystem1</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:ValueExpr></ns0:Expr></ns0:VariableDeclr><ns0:VariableDeclr><ns0:Text /><ns0:Properties /><ns0:Customisable><ns0:Var>CPU_CORES_RANGE_1-4</ns0:Var><ns0:Value><ns0:Value>2</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:Value><ns0:Expr><ns0:CompoundDomainExpr><ns0:Subexpression><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#greater_than</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>0</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Subexpression><ns0:Subexpression><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#less_than_or_equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>4</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Subexpression><ns0:LogicalOp>http://www.slaatsoi.org/coremodel#and</ns0:LogicalOp></ns0:CompoundDomainExpr></ns0:Expr></ns0:Customisable></ns0:VariableDeclr><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>CPU_SPEED_for_VirtualSystem1</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#cpu_speed</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem1</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>1800</ns0:Value><ns0:Datatype>http://www.slaatsoi.org/coremodel/units#MHz</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>CPU_CORES_for_VirtualSystem1</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#vm_cores</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem1</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>4</ns0:Value><ns0:Datatype>http://www.w3.org/2001/XMLSchema#integer</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed><ns0:Guaranteed><ns0:Text /><ns0:Properties /><ns0:State><ns0:ID>MEMORY_for_VirtualSystem1</ns0:ID><ns0:Priority xsi:nil=\"true\" /><ns0:Constraint><ns0:TypeConstraintExpr><ns0:Value><ns0:FuncExpr><ns0:Text /><ns0:Properties /><ns0:Operator>http://www.slaatsoi.org/resources#memory</ns0:Operator><ns0:Parameter><ns0:ID>VM_of_type_VirtualSystem1</ns0:ID></ns0:Parameter></ns0:FuncExpr></ns0:Value><ns0:Domain><ns0:SimpleDomainExpr><ns0:ComparisonOp>http://www.slaatsoi.org/coremodel#equals</ns0:ComparisonOp><ns0:Value><ns0:CONST><ns0:Value>2048</ns0:Value><ns0:Datatype>http://www.slaatsoi.org/coremodel/units#MB</ns0:Datatype></ns0:CONST></ns0:Value></ns0:SimpleDomainExpr></ns0:Domain></ns0:TypeConstraintExpr></ns0:Constraint></ns0:State></ns0:Guaranteed></ns0:AgreementTerm></ns0:SLATemplate>"
}
response = requests.post("%s/users/%s/slats" % (fedApiAddress, userUuid),
                         data=json.dumps(data),
                         headers={'Content-Type': 'application/json'})
print "register user SLAT: %d %s" % (response.status_code, response.reason)
if response.status_code != 201:
    print "register user SLAT failed: " + response.content
    exit()

location = response.headers["location"]
m = re.match(r".*/slats/(\d+)$", location)
if m:
    slatId = int(m.group(1))
print "User SLAT has been created. slatId: %d" % slatId

# create application
data = {"deploymentDesc": "",
        "attributes": {"slaId": slatId,
                       "userSLATemplateUrl": "users/%s/slats/%d" % (userUuid, slatId),
                       "app_data": None},
        "name": "TestApp1",
        "applicationOvf": ""}

response = requests.post("%s/users/%s/applications" % (fedApiAddress, userUuid),
                         data=json.dumps(data),
                         headers={'Content-Type': 'application/json'})
print "create application: %d %s" % (response.status_code, response.reason)
if response.status_code != 201:
    print "create application failed: " + response.content
    exit()

location = response.headers["location"]
m = re.match(r".*/applications/([\w-]+)$", location)
if m:
    appId = m.group(1)
print "Application has been created. appId: " + appId

# submit the application
data = {}
response = requests.put("%s/users/%s/applications/%s/submit" % (fedApiAddress, userUuid, appId),
                        data=json.dumps(data),
                        headers={'Content-Type': 'application/json'})
print "submit application: %d %s" % (response.status_code, response.reason)
if response.status_code != 204:
    print "submit application failed: " + response.content
    exit()

# get negotationId
response = requests.get("%s/users/%s/applications/%s" % (fedApiAddress, userUuid, appId))
if response.status_code != 200:
    print "get negotiationId failed: " + response.content
    exit()

appData = json.loads(response.content)
print "Application data:"
print appData
appAttrs = json.loads(appData["attributes"])
negotiationId = appAttrs["negotiationIds"][0]
print "negotiationId: " + negotiationId

# get proposals
response = requests.get(
    "%s/users/%s/slats/%d/negotiation/%s/proposals" % (fedApiAddress, userUuid, slatId, negotiationId))
if response.status_code != 200:
    print "get proposals failed: " + response.content
    exit()

proposals = json.loads(response.content)
print "Proposals:"
print proposals
m = re.match(r".*/proposals/(\d+)$", proposals[0]["uri"])
if m:
    proposalId = m.group(1)
print "proposalId: " + proposalId

# create agreement
data = {"appUuid":appId}
response = requests.post("%s/users/%s/slats/%d/negotiation/%s/proposals/%s/createAgreement"
                        % (fedApiAddress, userUuid, slatId, negotiationId, proposalId),
                        data=json.dumps(data),
                        headers={'Content-Type': 'application/json'})
print "createAgreement: %d %s" % (response.status_code, response.reason)
if response.status_code != 201:
    print "create agreement failed: " + response.content
    exit()


# start the application
data = {}
response = requests.put("%s/users/%s/applications/%s/start" % (fedApiAddress, userUuid, appId),
                        data=json.dumps(data),
                        headers={'Content-Type': 'application/json'})
print "start application: %d %s" % (response.status_code, response.reason)
if response.status_code != 204:
    print "start application failed: " + response.content
    exit()

# stop the application
response = requests.put("%s/users/%s/applications/%s/stop" % (fedApiAddress, userUuid, appId))
print "stop application: %d %s" % (response.status_code, response.reason)
if response.status_code != 204:
    print "stop application failed: " + response.content
    exit()

# kill the application
response = requests.put("%s/users/%s/applications/%s/kill" % (fedApiAddress, userUuid, appId))
print "kill application: %d %s" % (response.status_code, response.reason)
if response.status_code != 204:
    print "kill application failed: " + response.content
    exit()
