package oap.json;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.TreeTraversingParser;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.DataInput;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OapHoconFactory extends JsonFactory {
    private final UnaryOperator<Config> resolver;

    public OapHoconFactory() {
        resolver = Config::resolve;
    }

    private OapHoconFactory( JsonFactoryBuilder builder, UnaryOperator<Config> resolver ) {
        super( builder );
        this.resolver = resolver;
    }

    @Override
    public JsonFactoryBuilder rebuild() {
        UnaryOperator<Config> resolution = this::resolve;
        return new JsonFactoryBuilder( this ) {
            @Override
            public OapHoconFactory build() {
                return new OapHoconFactory( this, resolution );
            }
        };
    }

    @Override
    public OapHoconFactory copy() {
        return ( OapHoconFactory ) rebuild().build();
    }

    public static InputStream open( URL url ) throws IOException {
        return new SourceInputStream( url.openStream(), url );
    }

    protected Config resolve( Config config ) {
        return resolver.apply( config );
    }

    @Override
    public String getFormatName() {
        return "HOCON";
    }

    @Override
    protected JsonParser _createParser( ObjectReadContext context, IOContext io, Reader reader ) {
        Object source = reader instanceof SourceReader r ? r.source : io.contentReference().getRawContent();
        boolean close = io.isResourceManaged() || StreamReadFeature.AUTO_CLOSE_SOURCE.enabledIn( context.getStreamReadFeatures( _streamReadFeatures ) );
        try( Reader owned = close ? reader : new FilterReader( reader ) {
            @Override
            public void close() {}
        } ) {
            ConfigParseOptions defaults = ConfigParseOptions.defaults();
            ConfigParseOptions options = fixClassLoader( org.slf4j.LoggerFactory.getLogger( OapHoconFactory.class ), source, defaults );
            if( source instanceof URL || source instanceof File || source instanceof URI ) options = options.setOriginDescription( source.toString() );
            try( URLClassLoader loader = options.getClassLoader() != defaults.getClassLoader() && options.getClassLoader() instanceof URLClassLoader urlLoader ? urlLoader : null ) {
                var root = resolve( ConfigFactory.parseReader( owned, options ) ).root();
                return new HoconParser( toNode( root, context, 0 ), context, root );
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        } catch( URISyntaxException e ) {
            throw new IllegalArgumentException( e );
        } finally {
            io.close();
        }
    }

    @Override
    protected JsonParser _createParser( ObjectReadContext context, IOContext io, DataInput input ) {
        io.close();
        throw new UnsupportedOperationException( "Use an InputStream or Reader for HOCON" );
    }

    @Override
    protected JsonParser _createParser( ObjectReadContext context, IOContext io, InputStream input ) {
        Reader reader = new InputStreamReader( input, UTF_8 );
        return _createParser( context, io, input instanceof SourceInputStream source ? new SourceReader( reader, source.source ) : reader );
    }

    @Override
    protected JsonParser _createParser( ObjectReadContext context, IOContext io, byte[] input, int offset, int length ) {
        return _createParser( context, io, new ByteArrayInputStream( input, offset, length ) );
    }

    @Override
    protected JsonParser _createParser( ObjectReadContext context, IOContext io, char[] input, int offset, int length, boolean recyclable ) {
        return _createParser( context, io, new CharArrayReader( input, offset, length ) );
    }

    @Override
    protected tools.jackson.core.JsonGenerator _createGenerator( tools.jackson.core.ObjectWriteContext context, IOContext io, java.io.Writer output ) {
        throw new UnsupportedOperationException( "Generating HOCON is not supported" );
    }

    @Override
    protected tools.jackson.core.JsonGenerator _createUTF8Generator( tools.jackson.core.ObjectWriteContext context, IOContext io, java.io.OutputStream output ) {
        throw new UnsupportedOperationException( "Generating HOCON is not supported" );
    }

    private static List<String> numericKeys( ConfigObject object ) {
        if( object.isEmpty() ) return null;
        try {
            var keys = new ArrayList<>( object.keySet() );
            for( String key : keys ) Integer.parseInt( key );
            keys.sort( Comparator.comparingInt( Integer::parseInt ) );
            return keys;
        } catch( NumberFormatException e ) {
            return null;
        }
    }

    private static JsonNode toNode( ConfigValue value, ObjectReadContext context, int depth ) {
        context.streamReadConstraints().validateNestingDepth( depth );
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return switch( value.valueType() ) {
            case OBJECT -> {
                ConfigObject object = ( ConfigObject ) value;
                var keys = numericKeys( object );
                if( keys != null ) {
                    var array = factory.arrayNode();
                    for( String key : keys ) array.add( toNode( object.get( key ), context, depth + 1 ) );
                    yield array;
                }
                var node = factory.objectNode();
                object.forEach( ( key, child ) -> node.set( key, toNode( child, context, depth + 1 ) ) );
                yield node;
            }
            case LIST -> {
                var array = factory.arrayNode();
                for( var child : ( ConfigList ) value ) array.add( toNode( child, context, depth + 1 ) );
                yield array;
            }
            case STRING -> factory.stringNode( ( String ) value.unwrapped() );
            case BOOLEAN -> factory.booleanNode( ( Boolean ) value.unwrapped() );
            case NULL -> factory.nullNode();
            case NUMBER -> switch( value.unwrapped() ) {
                case Integer number -> factory.numberNode( number );
                case Long number -> factory.numberNode( number );
                case Number number -> factory.numberNode( number.doubleValue() );
                default -> throw new IllegalArgumentException( "Invalid HOCON number" );
            };
        };
    }

    private static final class SourceInputStream extends FilterInputStream {
        private final URL source;

        private SourceInputStream( InputStream input, URL source ) {
            super( input );
            this.source = source;
        }
    }

    private static final class SourceReader extends FilterReader {
        private final URL source;

        private SourceReader( Reader reader, URL source ) {
            super( reader );
            this.source = source;
        }
    }

    private static final class HoconParser extends TreeTraversingParser {
        private final ConfigObject root;

        private HoconParser( JsonNode node, ObjectReadContext context, ConfigObject root ) {
            super( node, context );
            this.root = root;
        }

        @Override
        public tools.jackson.core.TokenStreamLocation currentTokenLocation() {
            return currentLocation();
        }

        @Override
        public tools.jackson.core.TokenStreamLocation currentLocation() {
            var path = new ArrayList<tools.jackson.core.TokenStreamContext>();
            for( var context = streamReadContext(); context != null; context = context.getParent() ) path.add( context );
            ConfigValue value = root;
            for( var context : path.reversed() ) {
                if( context.inObject() && context.currentName() != null && value instanceof ConfigObject object ) {
                    var child = object.get( context.currentName() );
                    if( child != null ) value = child;
                } else if( context.inArray() && context.getCurrentIndex() >= 0 ) {
                    int index = context.getCurrentIndex();
                    if( value instanceof ConfigList list && index < list.size() ) value = list.get( index );
                    else if( value instanceof ConfigObject object ) {
                        var keys = numericKeys( object );
                        if( keys != null && index < keys.size() ) value = object.get( keys.get( index ) );
                    }
                }
            }
            var origin = value.origin();
            return new tools.jackson.core.TokenStreamLocation( tools.jackson.core.io.ContentReference.rawReference( true, origin.description() ), -1L, origin.lineNumber(), -1 );
        }
    }

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
