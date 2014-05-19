require 'pp'
require 'json'
require 'rest_client'
require 'capybara'

class Request
  def self.get(url)
    request(RestClient.method(:get), url, :accept => :json)
  end
  
  def self.post(url, params, ctype = :json)
    request(RestClient.method(:post), url, params, :content_type => ctype, :accept => :json)
  end
  
  def self.put(url, params, ctype = :json)
    request(RestClient.method(:put), url, params, :content_type => ctype, :accept => :json)
  end
  
  def self.delete(url)
    request(RestClient.method(:delete), url)
  end
  
  def self.request(mthd, url, *args)
    res = {}
    
    begin
      mthd.call(url, *args) do |response, request, result|
        if not response.empty?
          res[:response] = JSON.parse(response)
        else
          res[:response] = nil
        end
        res[:result] = result
        res[:code] = result.code
        res[:headers] = response.headers
      end
    rescue
      puts "ERROR?!?"
      pp $!
    end
    
    res
  end
end

def path2id(url)
  url.split('/').last.to_i
end

def res2id(res)
  path2id(res[:headers][:location])
end

def random_postfix(txt)
  txt + rand(10000).to_s
end

class Capybara::Node::Element
  def drag_by(x, y)
    wait_until {
      session.driver.browser.action.drag_and_drop_by(native, x, y).perform
    }
  end
end

class Filter
  attr_accessor :slider, :handle 
  
  def initialize(slider)
    @slider = slider
    @handle = @slider.find('.ui-slider-handle')
    @step = 20
    @width = @slider.native.style('width').slice(0...-2).to_i
  end
  
  def handle_position
    @handle.native.style('left').slice(0...-2).to_i
  end
  
  def reset
    while handle_position > 0
      pos = handle_position
      step = @step
      
      if pos - step < 0
        step = pos
      end
      
      @handle.drag_by(-step, 0)
    end
  end
  
  def set_to(str)
    browser = @handle.session.driver.browser
    browser.action.click_and_hold(@handle.native).perform
    begin
      using_wait_time(0.1) do
        until @slider.has_content? str
          original_pos = handle_position
          offset = 2
          while original_pos == handle_position
            browser.action.move_by(offset, 0).perform
            offset += 1
          end
        end
      end
    ensure
      browser.action.release.perform
    end
  end
end

class Filters
  def initialize(session)
    @session = session
  end
  
  def reset_all
    @session.all('.sla-providers-filters .slider').each do |slider|
      Filter.new(slider).reset
    end
  end
  
  def filter(name)
    selector = ".sla-providers-filters .slider:contains(\"#{name}\")"
    el = @session.find(selector)
    Filter.new(el)
  end
end