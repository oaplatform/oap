package oap.metrics;

import java.util.LinkedHashMap;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class Name {
    String line;
    public final String measurement;
    public final LinkedHashMap<String, String> tags = new LinkedHashMap<>();

    public Name( String measurement ) {
        line = measurement;
        this.measurement = measurement;
    }

    public Name tag( String name, String value ) {
        tags.put( name, value );
        line += ',' + name + "=" + value;
        return this;
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Name name = (Name) o;

        return line.equals( name.line );

    }

    @Override
    public int hashCode() {
        return line.hashCode();
    }

    @Override
    public String toString() {
        return line;
    }

    public boolean matches( String name ) {
        return line.equals( name );
    }
}
