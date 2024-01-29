package oap.tsv;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    public static void split( String line, List<String> list ) {
        Objects.requireNonNull( line );

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
