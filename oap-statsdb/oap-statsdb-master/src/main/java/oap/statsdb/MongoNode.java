package oap.statsdb;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * Created by igor.petrenko on 26.03.2019.
 */
@ToString
@EqualsAndHashCode( of = { "_id" } )
public class MongoNode {
    @SuppressWarnings( "checkstyle:MemberName" )
    public final Map<String, String> _id;
    public final Node n;

    @JsonCreator
    @SuppressWarnings( "checkstyle:ParameterName" )
    public MongoNode( Map<String, String> _id, Node n ) {
        this._id = _id;
        this.n = n;
    }
}
