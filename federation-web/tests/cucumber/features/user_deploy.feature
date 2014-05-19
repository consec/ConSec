Feature: User deploys application
  
  Background:
    Given base URL "http://localhost:8000"
      And API base URL "http://localhost:8080/federation-api"
      And provider "XLAB" @ "http://10.31.1.10:10500" exists
      And provider SLA "XLAB SLA" @ "http://contrail.xlab.si/test-files/ubuntu-test-xlab-SLA.xml" exists
      And provider server "n0010" exists
      And new user is registered
      And new user is logged in
  
  @user_deploy_app
  Scenario: User deploys application
    When I click on "SLA Negotiation"
      And I reset filters
      And I move filter "RAM total" to ">= 1.0 GB"
      And I move filter "RAM free" to ">= 512.0 MB"
      And I move filter "CPU cores" to ">= 1"
      And I move filter "CPU speed" to ">= 1.0 GHz"
      And I move filter "CPU load one" to "<= 0.8"
      And I move filter "CPU load five" to "<= 0.8"
      And I click on "XLAB"
      And I fill in "Name" with "My SLA"
      And I click on "XLAB SLA"
    Then I should see text "Ubuntu"
    When I click on "Negotiate"
    Then I should see text "has been created"
    
    When I click on "Register application"
      And I fill in "Application name" with "new_app"
      And I drag "My SLA" SLA to target
      And I click on "Register"
    Then I should see text "has been registered"
    
    When I click on "new_app"
      And I click on "Deploy application"
    Then I should see text "deployed" in next "60" seconds
    
    
