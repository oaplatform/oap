package oap.json;

import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import com.typesafe.config.ConfigParseOptions;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

public class OapHoconFactory extends HoconFactory {
    static URI getParent( URI uri ) throws URISyntaxException {
        String strUri = uri.toString();

        int index = strUri.lastIndexOf( "/" );

        String strParentUri = strUri.substring( 0, index + 1 );

        return new URI( strParentUri );
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    protected ConfigParseOptions fixClassLoader( Logger log, Object rawContent, ConfigParseOptions options ) throws MalformedURLException, URISyntaxException {
        switch( rawContent ) {
            case URL urlContext -> {
                URL parentURL = getParent( urlContext.toURI() ).toURL();
                log.trace( "parentURL {}", parentURL );
                options = options.setClassLoader( new URLClassLoader( new URL[] { parentURL } ) );
            }
            case File fileContext -> {
                URL parentURL = getParent( fileContext.toURI() ).toURL();
                log.trace( "parentURL {}", parentURL );
                options = options.setClassLoader( new URLClassLoader( new URL[] { parentURL } ) );
            }
            case URI uriContext -> {
                URL parentURL = getParent( uriContext ).toURL();
                log.trace( "parentURL {}", parentURL );
                options = options.setClassLoader( new URLClassLoader( new URL[] { parentURL } ) );
            }
            default -> {
            }
        }
        return options;
    }
}
