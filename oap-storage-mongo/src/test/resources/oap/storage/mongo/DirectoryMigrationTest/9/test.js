db.test.update({_id: "test"}, {$inc: {c: NumberInt(5)}});
db.test.update({_id: "test"}, {$inc: {c: NumberInt(-2)}});
