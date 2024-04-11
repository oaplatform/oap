package oap.logstream.data.map;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.logstream.data.LogRenderer;
import oap.reflect.Reflect;
import oap.template.BinaryOutputStream;
import oap.template.TemplateAccumulatorString;
import oap.util.Dates;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.logstream.data.TsvDataTransformer.ofBoolean;
import static oap.logstream.data.TsvDataTransformer.ofString;

@ToString
@EqualsAndHashCode
public class MapLogRenderer implements LogRenderer<Map<String, Object>, String, StringBuilder, TemplateAccumulatorString> {
    private final String[] headers;
    private final List<String> expressions;
    private final byte[][] types;

    public MapLogRenderer( String[] headers, byte[][] types, List<String> expressions ) {
        this.headers = headers;
        this.types = types;
        this.expressions = expressions;
    }

    @Nonnull
    @Override
    public String[] headers() {
        return headers;
    }

    @Nonnull
    @Override
    public byte[] render( @Nonnull Map<String, Object> data ) {
        var bos = new BinaryOutputStream( new ByteArrayOutputStream() );
        StringJoiner joiner = new StringJoiner( "\t" );
        joiner.add( Dates.FORMAT_SIMPLE_CLEAN.print( DateTime.now() ) );

        render( data, joiner );

        String line = joiner + "\n";
        return line.getBytes( UTF_8 );
    }

    @Override
    public byte[] render( @Nonnull Map<String, Object> data, StringBuilder sb ) {
        StringJoiner joiner = new StringJoiner( "\t" );

        render( data, joiner );

        sb.append( joiner );
        sb.append( "\n" );
        return sb.toString().getBytes( UTF_8 );
    }

    private void render( @Nonnull Map<String, Object> data, StringJoiner joiner ) {
        for( String expression : expressions ) {
            Object v = Reflect.get( data, expression );
            joiner.add( switch( v ) {
                case null -> "";
                case String str -> ofString( str );
                case Boolean b -> ofBoolean( b );
                default -> String.valueOf( v );
            } );
        }
    }

    @Override
    public byte[][] types() {
        return types;
    }
}
