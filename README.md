ConSec
======
ConSec is a federated cloud security framework.

# Architecture

The following diagram depicts an overview of the architecture of the ConSec framework:

![ConSec architecture](docs/consec-architecture.jpg)

# Short Description of the Components

## Identity Provider (IdP)
The Identity Provider is responsible for authentication of users and providing user's identity and 
attributes information in the form of SAML assertions to the Service Providers. In the architecture depicted in the 
upper diagram the Web Portal acts in the role of SAML Service Provider. The IdP enables single sign-on functionality.
The ConSec IdP utilizes the SimpleSAMLphp software operating in the Identity Provider mode with the MultiAuth module 
enabled which enables users to authenticate against various authentication sources.

## Federation REST API (federation-api)
The Federation REST API exposes federation database through REST interface.

## Federation Database (federation-db)
The Federation Database stores user profiles, roles, groups, attributes, identity providers.

## Web Portal
The Web Portal is a web application that provides some service to the users and uses ConSec framework for providing 
security.

## OAuth Authorization Server (OAuth Auth Server)
The OAuth Authorization Server issues access tokens to the clients after successfully authenticating the resource 
owner and obtaining authorization. The client uses the access token to access the protected resources hosted by the 
resource server.

## CA Server
The CA Server issues delegated user certificates to the clients. The clients can use the certificate to access the 
protected resources hosted by the resource server on behalf of the user. To request the certificate the client has 
to provide a valid access token.

## Resource Server
The Resource Server is the server hosting the protected resources, capable of accepting and responding to protected 
resource requests using access tokens. The ConSec provides three filters which can be plugged into the Resource 
Server (implemented as a Java web application) to enable specific functionality: OAuth Filter, Auditing Filter, 
and Authorization Filter. 

## OAuth Filter
The OAuth Filter is a servlet filter that can be plugged into java web application and provides support for the OAuth 
protocol to applications having Resource Server role in the OAuth flow. The filter intercepts the HTTP requests,
extracts OAuth access token from the request, validates the token and retrieves user information by calling OAuth 
Authorization Server and injects that data in the request (HttpServletRequest object).

## Auditing Filter
The Auditing Filter is a servlet filter that can be plugged into java web application and provides auditing support. 
For each incoming HTTP request an audit record is created and sent to the Auditing Storage Service. The degree of 
details of audit records can be configured and adjusted to the specific application.

## Authorization Filter
The Authorization Filter is a servlet filter that can be plugged into java web application. It acts as a Policy 
Enforcement Point (PEP) - it intercepts user's access request to a resource, makes a decision request to the PDP to 
obtain the access decision (i.e. access to the resource is approved or rejected), and acts on the received decision.

## Auditing Storage Service
The Auditing Storage Service listens and receives audit records and stores them to the auditing database. It is made 
modular and various communication channels for transporting audit records can be used as well as database systems.  

## Auditing API
The Auditing API provides REST interface for retrieving audit records from the auditing database using various search
criteria.

## Policy Decision Point (PDP)
The Policy Decision Point receives decision requests from the Policy Enforcement Point (PEP) containing all the 
relevant information about the request. The PDP evaluates the request using XACML authorization policies, 
makes an access control decision and returns it to the PEP which simply enforces the decision. The authorization 
policies are stored in the XACML Policy Repository. 
