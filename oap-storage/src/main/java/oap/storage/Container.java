package oap.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class Container<T, TMetadata extends Metadata<T>> {
    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "a:type")
    public TMetadata metadata;

    public Container( TMetadata metadata ) {
        this.metadata = metadata;
    }

    public Container() {
    }
}
