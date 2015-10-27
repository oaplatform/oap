package oap.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.joda.time.DateTimeUtils;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
@EqualsAndHashCode
@ToString
public class Metadata<T> implements Comparable<Metadata<T>> {
    public String id;
    //        @todo migration
    public int version = 0;
    public long modified = DateTimeUtils.currentTimeMillis();
    public boolean deleted;
    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "object:type")
    public T object;

    public Metadata( String id, T object ) {
        this.id = id;
        this.object = object;
    }

    public Metadata() {
    }

    @Override
    public int compareTo( Metadata<T> o ) {
        return this.id.compareTo( o.id );
    }

    public void update( T t ) {
        this.object = t;
        this.modified = DateTimeUtils.currentTimeMillis();
    }
}
