package oap.logstream.tsv;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by igor.petrenko on 2019-10-01.
 * @deprecated oap.tsv.TsvInputStream
 */
@Deprecated
public class TsvInputStream extends FastBufferedInputStream {
    private static final char TAB = '\t';
    private static final char ESCAPE = '\\';
    public final Line line;

    public TsvInputStream( InputStream is, byte[] bytes ) {
        super( is );

        line = new Line( bytes );
    }

    public static void split( byte[] line, int len, IntArrayList list ) {
        int i = 0;
        boolean escape = false;
        while( i < len ) {
            var ch = line[i];
            switch( ch ) {
                case ESCAPE -> escape = !escape;
                case TAB -> {
                    if( !escape ) list.add( i + 1 );
                    escape = false;
                }
                default -> escape = false;
            }
            i++;
        }
        list.add( i + 1 );
    }

    public static void split( String line, ArrayList<String> list ) {
        assert line != null;

        var len = line.length();

        int start = 0, i = 0;
        boolean escape = false;
        while( i < len ) {
            var ch = line.charAt( i );
            switch( ch ) {
                case ESCAPE -> escape = !escape;
                case TAB -> {
                    if( !escape ) list.add( line.substring( start, i ) );
                    start = i + 1;
                    escape = false;
                }
                default -> escape = false;
            }
            i++;
        }
        list.add( line.substring( start, i ) );
    }

    public static String escape( String text ) {
        if( text == null || text.length() == 0 ) return "";

        var sb = new StringBuilder();

        for( var i = 0; i < text.length(); i++ ) {
            var ch = text.charAt( i );
            switch( ch ) {
                case '\n' -> sb.append( "\\n" );
                case '\r' -> sb.append( "\\r" );
                case '\t' -> sb.append( "\\t" );
                case '\\' -> sb.append( "\\\\" );
                default -> sb.append( ch );
            }
        }

        return sb.toString();
    }

    public boolean readCells() throws IOException {
        line.cells.clear();
        var buffer = line.buffer;
        var len = readLine( buffer );

        line.len = len;

        if( len <= 0 ) return false;

        split( buffer, len, line.cells );

        return true;
    }

    @ToString
    public static class Line {
        public final byte[] buffer;
        public final IntArrayList cells = new IntArrayList();
        public int len = 0;

        public Line( byte[] buffer ) {
            this.buffer = buffer;
        }

        public int indexOf( String value ) {
            for( int i = 0; i < cells.size(); i++ ) {
                var offset = i == 0 ? 0 : cells.getInt( i - 1 );
                var length = cells.getInt( i ) - offset - 1;
                var str = new String( buffer, offset, length, UTF_8 );

                if( value.equals( str ) ) return i;
            }

            return -1;
        }
    }
}
