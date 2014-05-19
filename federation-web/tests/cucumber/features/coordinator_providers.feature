Feature: Coordinator providers

@coordinator_providers_list
Scenario: Coordinator opens Providers list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "Providers" in navigation bar
  And I should see text "Providers"

@coordinator_creates_provider
Scenario: Coordinator creates new provider
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/providers/create"
    And I fill in "Name" with "NewProvider" and random postfix
    And I fill in "Provider URI" with "http://10.31.1.10:10500/" and random postfix
    And I press "Create provider" button
  Then I should be redirected to "/federation/providers"
    And I should see text "Provider has been created"

@coordinator_edits_last_provider
Scenario: Coordinator edits last provider
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/providers"
    And I select "Edit" action on last row
  Then I should see text "Edit Provider"
  When I fill in "Name" with "NewProviderRenamed" and random postfix
    And I press "Save" button
  Then I should be redirected to "/federation/providers"
    And I should see text "Provider has been saved."

@coordinator_removes_last_provider
Scenario: Coordinator removes last provider
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/providers"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/providers"
    And I should see text "Provider was successfully removed."
