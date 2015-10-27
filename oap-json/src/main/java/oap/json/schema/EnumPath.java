package oap.json.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class EnumPath {
    private final String path;

    public EnumPath( String path ) {
        this.path = path;
    }

    @SuppressWarnings( "unchecked" )
    public List<Object> traverse( Object json ) {
        String[] paths = path.split( "\\." );

        Optional<Object> result = traverse( json, paths );

        return result.map( r -> {
            if( r instanceof List<?> ) return (List<Object>) r;

            return Collections.singletonList( r );
        } ).orElseGet( Collections::emptyList );

    }

    private Optional<Object> traverse( Object json, String[] paths ) {
        Object last = json;

        for( int i = 0; i < paths.length; i++ ) {
            String field = paths[i];

            if( last == null ) return Optional.empty();
            else if( last instanceof Map<?, ?> ) {
                Map<?, ?> map = (Map<?, ?>) last;

                last = map.get( field );
            } else if( last instanceof List<?> ) {
                List<?> list = (List<?>) last;

                ArrayList<Object> result = new ArrayList<>();

                for( Object item : list ) {
                    Optional<Object> value = traverse( item, Arrays.copyOfRange( paths, i, paths.length ) );
                    value.ifPresent( result::add );
                }

                last = result;

                i = paths.length;
            } else return Optional.empty();
        }

        return Optional.of( last );
    }
}
