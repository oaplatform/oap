package oap.http.server.nio;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.PathMatcher;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class PathHandlerHelper {
    private static final Field pathMatcherField;

    static {
        try {
            pathMatcherField = PathHandler.class.getDeclaredField( "pathMatcher" );
            pathMatcherField.setAccessible( true );
        } catch( NoSuchFieldException e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    @SneakyThrows
    public static PathMatcher<HttpHandler> getPathMatcher( PathHandler pathHandler ) {
        return ( PathMatcher<HttpHandler> ) pathMatcherField.get( pathHandler );
    }
}
