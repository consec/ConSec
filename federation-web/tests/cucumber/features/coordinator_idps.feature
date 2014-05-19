Feature: Coordinator idps

@coordinator_idps_list
Scenario: Coordinator opens Identity Providers list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "IDPs" in navigation bar
  And I should see text "Identity Providers"

@coordinator_creates_idp
Scenario: Coordinator creates new Identity Provider
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/idps/create"
    And I fill in "Name" with "ID provider" and random postfix
    And I fill in "URI" with "http://id.provider.org/"
    And I fill in "Description" with "ID Provider description"
    And I press "Create Identity Provider" button
  Then I should be redirected to "/federation/idps"
    And I should see text "Identity Provider has been created"

@coordinator_edits_last_idp
Scenario: Coordinator edits last Identity provider
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/idps"
    And I select "Edit" action on last row
  Then I should see text "Edit Identity Provider"
  When I fill in "Name" with "ID provider updated" and random postfix
    And I press "Save" button
  Then I should be redirected to "/federation/idps"
    And I should see text "Identity Provider has been saved."

@coordinator_removes_last_idp
Scenario: Coordinator removes last idp
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/idps"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/idps"
    And I should see text "Identity Provider was successfully removed."
