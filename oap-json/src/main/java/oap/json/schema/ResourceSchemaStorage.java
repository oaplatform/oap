package oap.json.schema;

import oap.io.Resources;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class ResourceSchemaStorage implements SchemaStorage {
    @Override
    public String get( String name ) {
        return Resources.readString( getClass(), name )
                .orElseThrow( () -> new IllegalArgumentException( "not found " + name ) );
    }
}
