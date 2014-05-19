Feature: Coordinator attributes

@coordinator_attributes_list
Scenario: Coordinator opens Attributes list
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I click on "Attributes" in navigation bar
  And I should see text "Attributes"

@coordinator_creates_attribute
Scenario: Coordinator creates new attribute
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/attributes/create"
    And I fill in "Name" with "new:attribute" and random postfix
    And I fill in "URI" with "http://www.contrail-project.org/attribute/new:attribute"
    And I fill in "Default value" with "000000000"
    And I fill in "Reference" with "users"
    And I fill in "Description" with "Description"
    And I press "Create attribute" button
  Then I should be redirected to "/federation/attributes"
    And I should see text "Attribute has been created"

@coordinator_edits_last_attribute
Scenario: Coordinator edits last attribute
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/attributes"
    And I select "Edit" action on last row
  Then I should see text "Edit Attribute"
  When I fill in "Name" with "new:attribute:updated" and random postfix
    And I press "Save" button
  Then I should be redirected to "/federation/attributes"
    And I should see text "Attribute has been saved."

@coordinator_removes_last_attribute
Scenario: Coordinator removes last attribute
  Given base URL "http://localhost:8000"
    And logged in with username "coordinator" and password "password"
  When I go to "/federation/attributes"
    And I select "Remove" action on last row
  Then I should be redirected to "/federation/attributes"
    And I should see text "Attribute was successfully removed."
