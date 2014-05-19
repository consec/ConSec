When /^I fill in "([^"]*)" with last username$/ do |arg1|
  fill_in arg1, :with => @form['Username']
end

When /^I fill in "([^"]*)" with last password$/ do |arg1|
  fill_in arg1, :with => @form['Password']
end
