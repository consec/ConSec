$(function () {
	$('.delete-action').click(function(){
		return confirm('Are you sure?');
	});
	
	var next_el_id = 1;
	
    $.fn.uid = function(prefix) { 
        return this.each(function() {
            if (!this.id) {
                this.id = (prefix || 'el-') + (next_el_id++);
            }
            return $;
        }); 
    };
});

_.allTrue = function(arr){
    return _.all(arr, function(x){
        return !!x;
    })
};
