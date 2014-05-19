Feature: Coordinator roles

@coordinator_roles_list
Scenario: Coordinator opens Roles list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "Roles" in navigation bar
  And I should see text "Roles"

@coordinator_creates_role
Scenario: Coordinator creates new role
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/roles/create"
    And I fill in "Name" with "New role" and random postfix
    And I fill in "Description" with "Description"
    And I fill in "ACL" with "ACL"
    And I press "Create role" button
  Then I should be redirected to "/federation/roles"
    And I should see text "Role has been created"

@coordinator_edits_last_role
Scenario: Coordinator edits last role
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/roles"
    And I select "Edit" action on last row
  Then I should see text "Edit Role"
  When I fill in "Name" with "New role updated" and random postfix
    And I press "Save" button
  Then I should be redirected to "/federation/roles"
    And I should see text "Role has been saved."

@coordinator_removes_last_role
Scenario: Coordinator removes last role
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/roles"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/roles"
    And I should see text "Role was successfully removed."
