package oap.io;

import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

public class StringStreamInputStream extends InputStream {
    private Iterator<String> iterator;
    private String delimiter;
    private Buffer buffer = new Buffer();

    public StringStreamInputStream( Stream<String> stream ) {
        this( stream, null );
    }

    public StringStreamInputStream( Stream<String> stream, String delimiter ) {
        this.iterator = stream.iterator();
        this.delimiter = delimiter;
    }

    @Override
    public int read() {
        if( !tryAdvance() ) return -1;
        return buffer.next();
    }


    @Override
    public int read( byte[] b, int off, int len ) {
        if( b == null ) throw new NullPointerException();
        else if( off < 0 || len < 0 || len > b.length - off ) throw new IndexOutOfBoundsException();
        else if( len == 0 ) return 0;

        if( !tryAdvance() ) return -1;

        int i = 0;
        for( ; i < len && buffer.hasNext(); i++ ) b[off + i] = ( byte ) buffer.next();
        return i;
    }

    private boolean tryAdvance() {
        if( buffer.active() && !buffer.hasNext() ) buffer.clear();
        if( !buffer.active() && !iterator.hasNext() ) return false;
        if( !buffer.active() ) buffer.reset( iterator.next(), iterator.hasNext() ? delimiter : null );
        return true;
    }

    private static class Buffer {
        private String value;
        private String delimiter;
        int position = 0;

        int length() {
            return value.length() + ( delimiter != null ? delimiter.length() : 0 );
        }

        void reset( String value, String delimiter ) {
            this.value = value;
            this.delimiter = delimiter;
            this.position = 0;
        }

        boolean active() {
            return this.value != null;
        }

        void clear() {
            this.value = null;
        }

        char next() {
            if( position < value.length() ) return value.charAt( position++ );
            else if( delimiter != null ) return delimiter.charAt( -value.length() + position++ );
            throw new IndexOutOfBoundsException( value + " at " + position );
        }

        boolean hasNext() {
            return position < length();
        }
    }
}
