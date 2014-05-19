Feature: Coordinator views dashboard

Scenario: Coordinator signs in
  Given base URL "http://localhost:8000"
  When I go to "/login"
    And I fill in "username" with "coordinator"
    And I fill in "password" with "password"
    And I press "Sign in" button
  Then I should be redirected to "/federation"
  And I should see text "Hey, Coordinator"

Scenario: Coordinator login fails
  Given base URL "http://localhost:8000"
  When I go to "/login"
    And I fill in "username" with "coordinator"
    And I fill in "password" with "invalid"
    And I press "Sign in" button
  Then I should see text "Please enter a correct username and password"
