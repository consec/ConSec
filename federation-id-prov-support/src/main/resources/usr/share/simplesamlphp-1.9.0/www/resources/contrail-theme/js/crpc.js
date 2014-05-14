var CRpc = {
    get_cookie: function(name){
        var cookieValue = null;
        if (document.cookie && document.cookie != '') {
            var cookies = document.cookie.split(';');
            for (var i = 0; i < cookies.length; i++) {
                var cookie = cookies[i].toString().replace(/^\s+/, "").replace(/\s+$/, "");
                // Does this cookie string begin with the name we want?
                if (cookie.substring(0, name.length + 1) == (name + '=')) {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    },

    call: function(url, action, success_callback, argv, custom_settings){
        success_callback = success_callback || Dajax.process;
        custom_settings = custom_settings || {};
        
        var error_callback = this.get_setting('default_exception_callback');
        
        if('error_callback' in custom_settings && typeof(custom_settings['error_callback']) == 'function'){
            error_callback = custom_settings['error_callback'];
        }
    	
        $.ajax(url, {
        	cache: false,
        	data: {
        		action: action,
        		argv: JSON.stringify(argv)
    		},
        	headers: {
        		'X-Requested-With': 'XMLHttpRequest',
        		'X-CSRFToken': CRpc.get_cookie('csrftoken')
        	},
        	success: function(data){
        		success_callback(data);
        	},
        	type: 'POST'
        });
    },

    setup: function(settings){
        this.settings = settings;
    },

    get_setting: function(key){
        if(this.settings == undefined || this.settings[key] == undefined){
            return this.default_settings[key];
        }
        return this.settings[key];
    },

    default_exception_callback: function(data){
        alert('Something went wrong');
    },

    valid_http_responses: function(){
        return {200: null, 301: null, 302: null, 304: null}
    }
};

CRpc.default_settings = {'default_exception_callback': CRpc.default_exception_callback}

window['CRpc'] = CRpc;
