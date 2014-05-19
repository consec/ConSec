Feature: Coordinator users

@coordinator_users_list
Scenario: Coordinator opens Users list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "Users" in navigation bar
  And I should be redirected to "/federation/users"

@coordinator_creates_federation_user
Scenario: Coordinator created new Federation user
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/users/create"
    And I fill in "Username" with "new_user" and random postfix
    And I fill in "Password" with "password"
    And I fill in "Email" with "foo@bar.baz"
    And I fill in "First name" with "New"
    And I fill in "Last name" with "User"
    And I select "Roles" "FederationUser"
    And I press "Create user" button
  Then I should be redirected to "/federation/users"
    And I should see text "User has been created"

@coordinator_creates_cloud_administrator
Scenario: Coordinator creates new Cloud Administrator
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/users/create"
    And I fill in "Username" with "new_provider" and random postfix
    And I fill in "Password" with "password"
    And I fill in "Email" with "foo@bar.baz"
    And I fill in "First name" with "New"
    And I fill in "Last name" with "User"
    And I select "Roles" "CloudAdministrator"
    And I press "Create user" button
  Then I should be redirected to "/federation/users"
    And I should see text "User has been created"

@coordinator_edits_last_user
Scenario: Coordinator edits last user
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/users"
    And I select "Edit" action on last row
  Then I should see text "Edit User"
  When I fill in "First name" with "Name"
    And I press "Save" button
  Then I should be redirected to "/federation/users"
    And I should see text "User has been saved."

@coordinator_removes_last_user
Scenario: Coordinator removes last user
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/users"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/users"
    And I should see text "User was successfully removed."
