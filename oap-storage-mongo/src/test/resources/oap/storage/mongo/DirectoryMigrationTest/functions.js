var incC = function(collection, id, field, count) {
    var params = {}
    params[field] = count;
    collection.update({_id: id}, {$inc: params});
}