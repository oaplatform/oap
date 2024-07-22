package oap.http;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class Https {
    @SuppressWarnings( { "checkstyle:UnnecessaryParentheses", "checkstyle:ParameterAssignment" } )
    public static String replaceHostInUrl( String url, String newHost ) {
        if( url == null || newHost == null ) {
            return url;
        }

        try {
            URI originalURI = new URI( url );

            boolean hostHasPort = newHost.contains( ":" );
            int newPort;
            if( hostHasPort ) {
                URI hostURI = new URI( "http://" + newHost );
                newHost = hostURI.getHost();
                newPort = hostURI.getPort();
            } else {
                newPort = -1;
            }

            // Use implicit port if it's a default port
            boolean isHttps = originalURI.getScheme().equals( "https" );
            boolean useDefaultPort = ( newPort == 443 && isHttps ) || ( newPort == 80 && !isHttps );
            newPort = useDefaultPort ? -1 : newPort;

            URI newURI = new URI( originalURI.getScheme().toLowerCase(), null, newHost, newPort, originalURI.getPath(), originalURI.getQuery(), originalURI.getFragment() );
            String result = newURI.toString();

            return result;
        } catch( URISyntaxException e ) {
            log.error( e.getMessage(), e );
            throw new RuntimeException( "Couldnt replace host in url, originalUrl=" + url + ", newHost=" + newHost );
        }
    }
}
