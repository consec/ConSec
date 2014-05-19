BUILD_ID = ENV['BUILD_NUMBER'] || 1

$LOAD_PATH << File.expand_path('../../../src', __FILE__)
require 'WebTestUtil.rb'

require 'capybara'
require 'capybara/dsl'

include Test::Unit::Assertions
include Capybara::DSL

Capybara.default_driver = :selenium
Capybara.run_server = false

Before do
  reset_session!
  @form = {}
end