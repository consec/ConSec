Feature: User registers

@auth_user_registers
Scenario: User registers
  Given base URL "http://localhost:8000"
  When I go to "/registration"
    And I fill in "First name" with "Foo"
    And I fill in "Last name" with "Bar"
    And I fill in "Username" with "foobar" and random postfix
    And I fill in "E-mail" with "foo@bar.baz"
    And I fill in "Password" with "password"
    And I fill in "Password (again)" with "password"
    And I press "Sign up" button
  Then I should be redirected to "/login"
  When I go to "/login"
    And I fill in "Username" with last username
    And I fill in "Password" with last password
    And I press "Sign in" button
  Then I should be redirected to "/user"

@auth_user_registers_with_taken_username
Scenario: User enters already taken username
  Given base URL "http://localhost:8000"
  When I go to "/registration"
    And I fill in "First name" with "Foo"
    And I fill in "Last name" with "Bar"
    And I fill in "Username" with "foobar" and random postfix
    And I fill in "E-mail" with "foo@bar.baz"
    And I fill in "Password" with "password"
    And I fill in "Password (again)" with "password"
    And I press "Sign up" button
  Then I should be redirected to "/login"
  When I go to "/registration"
    And I fill in same data as last time
    And I press "Sign up" button
  Then I should see text "Registration failed"

@auth_user_registers_with_wrong_password
Scenario: User repeats wrong password
  Given base URL "http://localhost:8000"
  When I go to "/registration"
    And I fill in "First name" with "Foo"
    And I fill in "Last name" with "Bar"
    And I fill in "Username" with "foobar" and random postfix
    And I fill in "E-mail" with "foo@bar.baz"
    And I fill in "Password" with "password"
    And I fill in "Password (again)" with "foo"
    And I press "Sign up" button
  Then I should see text "The two password fields didn't match"

@auth_user_registers_with_invalid_email
Scenario: User enters invalid email
  Given base URL "http://localhost:8000"
  When I go to "/registration"
    And I fill in "First name" with "Foo"
    And I fill in "Last name" with "Bar"
    And I fill in "Username" with "foobar" and random postfix
    And I fill in "E-mail" with "foo"
    And I fill in "Password" with "password"
    And I fill in "Password (again)" with "password"
    And I press "Sign up" button
  Then I should see text "Enter a valid e-mail address"
