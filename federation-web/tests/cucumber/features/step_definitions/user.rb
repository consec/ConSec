Given /^provider "([^"]*)" @ "([^"]*)" exists$/ do |arg1, arg2|
  url = @apibaseurl + '/providers'
  
  res = Request.get(url)
  
  provider = res[:response].find { |x| x['name'] == arg1 }
  
  if provider
    @providerid = path2id(provider['uri'])
    @providerurl = url + '/' + @providerid.to_s
  else
    provider_data = {
      'name' => arg1,
      'typeId' => 42,
      'providerUri' => arg2,
    }.to_json
    
    res = Request.post(url, provider_data)
    
    assert (['201'].include? res[:code]), res.to_s
    
    @providerid = res2id(res)
    @providerurl = url + '/' + @providerid.to_s
  end
end

Given /^provider SLA "([^"]*)" @ "([^"]*)" exists$/ do |arg1, arg2|
  url = @providerurl + '/slats'
  
  res = Request.get(url)
  
  slat = res[:response].find { |x| x['name'] == arg1 }
  
  if slat
    @providerslaid = path2id(slat['uri'])
    @providerslaurl = url + '/' + @providerslaid.to_s
  else
    slat_data = {
      'name' => arg1,
      'url' => arg2,
    }.to_json
    
    res = Request.post(url, slat_data)
    
    assert (['201'].include? res[:code]), res.to_s
    
    @providerslaid = res2id(res)
    @providerslaurl = url + '/' + @providerslaid.to_s
  end
end

Given /^provider server "([^"]*)" exists$/ do |arg1|
  url = @providerurl + '/servers'
  
  res = Request.get(url)
  
  server = res[:response].find { |x| x['name'] == arg1 }
  
  if server
    @providerserverid = path2id(server['uri'])
    @providerserverurl = url + '/' + @providerserverid.to_s
  else
    server_data = {
      'name' => arg1,
      'ram_total' => '3915',
      'ram_used' => '1152',
      'ram_free' => '2763',
      'cpu_cores' => '4',
      'cpu_speed' => '2494.276',
      'cpu_load_one' => '0.09',
      'cpu_load_five' => '0.04'
    }.to_json
    
    res = Request.post(url, server_data)
    
    assert (['201'].include? res[:code]), res.to_s
    
    @providerserverid = res2id(res)
    @providerserverurl = url + '/' + @providerserverid.to_s
  end
end

Given /^new user is registered$/ do
  @username = random_postfix('new_user')
  @password = 'password'
  
  visit @baseurl
  click_on 'Registration'
  fill_in 'First name', :with => 'New'
  fill_in 'Last name', :with => 'User'
  fill_in 'Username', :with => @username
  fill_in 'E-mail', :with => 'new@user.com'
  fill_in 'Password', :with => @password
  fill_in 'Password (again)', :with => @password
  click_on 'Sign up'
  
  page.should have_content('Registration successful')
end

Given /^new user is logged in$/ do
  visit @baseurl
  fill_in 'Username', :with => @username
  fill_in 'Password', :with => @password
  click_on 'Sign in'
end

When /^I reset filters$/ do
  @filters = Filters.new(page)
  
  begin
    @filters.reset_all
  rescue => e
    pp e
  end
end

When /^I move filter "([^"]*)" to "([^"]*)"$/ do |arg1, arg2|
  @filters.filter(arg1).set_to(arg2)
end

When /^I drag "([^"]*)" SLA to target$/ do |arg1|
  sla = find(%{.ui-draggable:contains("#{arg1}")})
  target = find('.drop-target')
  sla.drag_to(target)
end
