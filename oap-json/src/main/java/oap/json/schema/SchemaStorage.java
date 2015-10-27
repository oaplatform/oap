package oap.json.schema;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
@FunctionalInterface
public interface SchemaStorage {
    String get( String name );
}
