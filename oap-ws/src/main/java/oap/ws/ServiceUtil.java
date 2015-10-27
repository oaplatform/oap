package oap.ws;

import oap.util.Stream;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
final class ServiceUtil {
    private static Pattern rxParamPattern = Pattern.compile( "\\{([^:]+):([^\\)]+\\))\\}" );
    private static Pattern namedParamPattern = Pattern.compile( "/\\{([^\\}]+)\\}" );

    public static Pattern compile( String mapping ) {
        return Pattern.compile( '^' +
            namedParamPattern.matcher( rxParamPattern.matcher( mapping ).replaceAll( "$2" ) )
                .replaceAll( "/([^/]+)" ) + '$' );
    }

    private static String filter( String mapping ) {
        return rxParamPattern.matcher( mapping ).replaceAll( "{$1}" );
    }

    public static Optional<String> pathParam( String mapping, String requestLine, String name ) {
        Matcher matcher = namedParamPattern.matcher( filter( mapping ) );
        return Stream.of( matcher::find, matcher::group )
            .zipWithIndex()
            .filter( p -> p._1.equals( "/{" + name + "}" ) )
            .map( p -> p._2 )
            .findFirst()
            .flatMap( group -> {
                Matcher matcher1 = compile( mapping ).matcher( requestLine );
                return matcher1.matches() && group <= matcher1.groupCount() ?
                    Optional.of( matcher1.group( group + 1 ) ) :
                    Optional.<String>empty();
            } );
    }
}
