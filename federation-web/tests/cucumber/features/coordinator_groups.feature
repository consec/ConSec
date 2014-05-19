Feature: Coordinator groups

@coordinator_groups_list
Scenario: Coordinator opens Groups list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "Groups" in navigation bar
  And I should see text "Groups"

@coordinator_creates_group
Scenario: Coordinator creates new group
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/groups/create"
    And I fill in "Name" with "New group" and random postfix
    And I press "Create group" button
  Then I should be redirected to "/federation/groups"
    And I should see text "Group has been created"

@coordinator_edits_last_group
Scenario: Coordinator edits last group
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/groups"
    And I select "Edit" action on last row
  Then I should see text "Edit Group"
  When I fill in "Name" with "New group updated" and random postfix
    And I press "Save" button
  Then I should be redirected to "/federation/groups"
    And I should see text "Group has been saved."

@coordinator_removes_last_group
Scenario: Coordinator removes last group
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/groups"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/groups"
    And I should see text "Group was successfully removed."
