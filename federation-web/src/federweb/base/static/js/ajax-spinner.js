$body = $("body");

$(document).on({
    ajaxStart: function() { $body.addClass("ajax-spinner-class");    },
     ajaxStop: function() { $body.removeClass("ajax-spinner-class"); }
});
