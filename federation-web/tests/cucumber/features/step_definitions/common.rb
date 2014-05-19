Given /^base URL "([^"]*)"$/ do |arg1|
  @baseurl = arg1
end

Given /^API base URL "([^"]*)"$/ do |arg1|
  @apibaseurl = arg1
end

When /^I go to "([^"]*)"$/ do |arg1|
  visit @baseurl + arg1
end

When /^I go to dashboard$/ do |arg1|
  visit @baseurl + '/'
end

When /^I fill in "([^"]*)" with "([^"]*)"$/ do |arg1, arg2|
  fill_in arg1, :with => arg2
  @form[arg1] = arg2
end

When /^I fill in "([^"]*)" with "([^"]*)" and random postfix$/ do |arg1, arg2|
  arg2 = random_postfix(arg2)
  fill_in arg1, :with => arg2
  @form[arg1] = arg2
end

When /^I press "([^"]*)" button$/ do |arg1|
  click_button arg1
end

Then /^I should be redirected to "([^"]*)"$/ do |arg1|
  current_path.should == arg1
end

When /^I fill in same data as last time$/ do
  @form.each do |key, value|
    fill_in key, :with => value
  end
end

Then /^I should see text "([^"]*)"$/ do |arg1|
  page.should have_content(arg1)
end

Given /^logged in with username "([^"]*)" and password "([^"]*)"$/ do |arg1, arg2|
  visit @baseurl + '/login'
  fill_in 'Username', :with => arg1
  fill_in 'Password', :with => arg2
  click_on 'Sign in'
end

When /^I select "([^"]*)" "([^"]*)"$/ do |arg1, arg2|
  select arg2, :from => arg1
end

When /^I select "([^"]*)" action on last row$/ do |arg1|
  find('table tr:last a.dropdown-toggle').click()
  click_link(arg1)
end

When /^I click on "([^"]*)" in navigation bar$/ do |arg1|
  within '.navbar-fixed-top' do
    click_link arg1
  end
end

When /^I click on "([^"]*)"$/ do |arg1|
  click_link_or_button arg1
end

Then /^I should see text "([^"]*)" in next "([^"]*)" seconds$/ do |arg1, arg2|
  using_wait_time(arg2.to_i) do
    page.should have_content(arg1)
  end
end

Then /^I should get a shell to play$/ do
  require 'ripl'
  Ripl.start :binding => binding
end

