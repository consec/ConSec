#!/bin/bash

curl -X POST --header "Content-Type: application/soap+xml" -d @samlquery.xml http://localhost:8080/federation-id-prov/saml
