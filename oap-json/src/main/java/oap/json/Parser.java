/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.json;


import oap.io.Files;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static <T> T parse( Path path ) {
        return parse( Files.readString( path ) );
    }


    @SuppressWarnings( "unchecked" )
    public static <T> T parse( String json ) {
        return (T) parse( new Lexer.Buffer( new StringReader( json ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T parse( Reader reader ) {
        return (T) parse( new Lexer.Buffer( reader ) );
    }

    public static <T> Optional<T> parseOpt( String json ) {
        try {
            return Optional.ofNullable( parse( json ) );
        } catch( Exception e ) {
            return Optional.empty();
        }
    }


    public static <T> Optional<T> parseOpt( Reader reader ) {
        try {
            return Optional.ofNullable( parse( reader ) );
        } catch( Exception e ) {
            return Optional.empty();
        }
    }


    @SuppressWarnings( "unchecked" )
    private static <T> T parse( Lexer.Buffer buf ) {
        try {
            return (T) parse( new Lexer( buf ) );
        } catch( JsonException e ) {
            throw e;
        } catch( Exception e ) {
            throw new JsonException( "parsing failed", e );
        } finally {
            buf.release();
        }
    }

    public static String unquote( char ch, String string ) {
        return unquote( ch, new Lexer.Buffer( new java.io.StringReader( string ) ) );
    }

    private static String unquote0( char ch, Lexer.Buffer buf, String base ) {
        StringBuilder s = new StringBuilder( base );
        char c = '\\';
        while( c != ch ) {
            if( c == '\\' ) {
                switch( buf.next() ) {
                    case '"':
                        if( ch == '"' )
                            s.append( '"' );
                        else
                            s.append( '\\' );
                        break;
                    case '\'':
                        if( ch == '\'' )
                            s.append( '\'' );
                        else
                            s.append( '\\' );
                        break;
                    case '\\':
                        s.append( '\\' );
                        break;
                    case '/':
                        s.append( '/' );
                        break;
                    case 'b':
                        s.append( '\b' );
                        break;
                    case 'f':
                        s.append( '\f' );
                        break;
                    case 'n':
                        s.append( '\n' );
                        break;
                    case 'r':
                        s.append( '\r' );
                        break;
                    case 't':
                        s.append( '\t' );
                        break;
                    case 'u':
                        char[] chars = { buf.next(), buf.next(), buf.next(), buf.next() };
                        int codePoint = Integer.parseInt( new String( chars ), 16 );
                        s.appendCodePoint( codePoint );
                        break;
                    default:
                        s.append( '\\' );
                }
            } else s.append( c );
            c = buf.next();
        }
        return s.toString();
    }

    private static String unquote( char ch, Lexer.Buffer buf ) {


        buf.mark();
        char c = buf.next();
        while( c != ch ) {
            if( c == '\\' ) {
                return unquote0( ch, buf, buf.substring() );
            }
            c = buf.next();
        }
        return buf.substring();
    }

    private static Object parse( Lexer p ) {
        ParserStack st = new ParserStack();
        Lexer.Token token;
        do {
            token = p.nextToken();
            switch( token.type ) {
                case BOOL:
                    st.pushValue( token.value );
                    break;
                case NULL:
                    st.pushValue( null );
                    break;
                case INTEGER:
                    st.pushValue( token.value );
                    break;
                case REAL:
                    st.pushValue( token.value );
                    break;
                case STRING:
                    st.pushValue( token.value );
                    break;
                case ARRAY_OPEN:
                    st.openArray();
                    break;
                case ARRAY_CLOSE:
                    st.closeArray();
                    break;
                case OBJECT_OPEN:
                    st.openObject();
                    break;
                case OBJECT_CLOSE:
                    st.closeObject();
                    break;
                case FIELD_START:
                    st.objectField( (String) token.value );
                    break;
            }
        } while( token.type != Lexer.Token.Type.END );
        return st.result();
    }

    private static class ParserStack {
        private LinkedList<Object> stack = new LinkedList<>();

        @SuppressWarnings( "unchecked" )
        public void pushValue( Object v ) {
            Object top = stack.peek();
            if( top instanceof LinkedList ) ((LinkedList<Object>) top).push( v );
            else if( top instanceof FieldName ) {
                stack.remove();
                Map<String, Object> map = (Map<String, Object>) stack.peek();
                map.put( ((FieldName) top).name, v );
            } else stack.addFirst( v );
        }

        public void openObject() {
            stack.addFirst( new LinkedHashMap<String, Object>() );
        }

        static class FieldName {
            String name;

            FieldName( String name ) {
                this.name = name;
            }
        }

        public void objectField( String name ) {
            pushValue( new FieldName( name ) );
        }

        @SuppressWarnings( "unchecked" )
        public void closeObject() {
            try {
                Map<String, Object> map = (Map<String, Object>) stack.remove();
                pushValue( map );
            } catch( ClassCastException e ) {
                fail( e );
            }
        }

        public void openArray() {
            stack.addFirst( new LinkedList() );
        }

        public void closeArray() {
            try {
                LinkedList l = (LinkedList) stack.remove();
                Collections.reverse( l );
                pushValue( l );
            } catch( ClassCastException e ) {
                fail( e );
            }
        }

        public Object result() {
            return (stack.size() != 1) ? fail( null ) : stack.getFirst();
        }

        public JsonException fail( Exception e ) {
            throw new JsonException( "cannot parse. \nStack:\n" + stack, e );
        }
    }

    private static char EOF = (char) -1;

    public static class Lexer {
        Buffer buf;

        public Lexer( String json ) {
            this( new Buffer( new StringReader( json ) ) );
        }

        Lexer( Buffer buf ) {
            this.buf = buf;
        }

        private LinkedList<BlockMode> blocks = new LinkedList<>();
        private boolean fieldNameMode = true;

        private JsonException fail( String msg ) {
            throw new JsonException( msg + "\nNear: " + buf.near(), null );
        }

        static boolean isDelimiter( char c ) {
            return c == ' ' || c == '\n' || c == ',' || c == '\r' || c == '\t' || c == '}' || c == ']';
        }

        String parseFieldName( char ch ) {
            buf.mark();
            char c = buf.next();
            while( c != EOF ) {
                if( c == ch ) return buf.substring();
                c = buf.next();
            }
            throw fail( "expected string end" );
        }

        String parseString( char ch ) {
            try {
                return unquote( ch, buf );
            } catch( Throwable e ) {
                throw fail( "unexpected string end" );
            }
        }

        Token parseValue( char first ) {
            boolean wasInt = true;
            boolean doubleVal = false;
            StringBuilder s = new StringBuilder();
            s.append( first );
            while( wasInt ) {
                char c = buf.next();
                if( c == '.' || c == 'e' || c == 'E' ) {
                    doubleVal = true;
                    s.append( c );
                } else if( !(Character.isDigit( c ) || c == '-') ) {
                    wasInt = false;
                    buf.back();
                } else s.append( c );
            }
            String value = s.toString();
            if( doubleVal ) return Token.constant( Token.Type.REAL, Double.parseDouble( value ) );
            else return Token.constant( Token.Type.INTEGER, Long.parseLong( value ) );
        }

        /**
         * Parse next Token from stream.
         */
        public Token nextToken() {
            while( true ) {
                char c = buf.next();
                switch( c ) {
                    case (char) -1:
                        return Token.END;
                    case '{':
                        blocks.addFirst( BlockMode.OBJECT );
                        fieldNameMode = true;
                        return Token.OBJECT_OPEN;
                    case '}':
                        blocks.poll();
                        return Token.OBJECT_CLOSE;
                    case '"':
                    case '\'':
                        if( fieldNameMode && blocks.peek() == BlockMode.OBJECT )
                            return Token.fieldStart( parseFieldName( c ) );
                        else {
                            fieldNameMode = true;
                            return Token.constant( Token.Type.STRING, parseString( c ) );
                        }
                    case 't':
                        fieldNameMode = true;
                        if( buf.next() == 'r' && buf.next() == 'u' && buf.next() == 'e' ) {
                            return Token.constant( Token.Type.BOOL, true );
                        }
                        fail( "expected boolean" );
                    case 'f':
                        fieldNameMode = true;
                        if( buf.next() == 'a' && buf.next() == 'l' && buf.next() == 's' && buf.next() == 'e' ) {
                            return Token.constant( Token.Type.BOOL, false );
                        }
                        fail( "expected boolean" );
                    case 'n':
                        fieldNameMode = true;
                        if( buf.next() == 'u' && buf.next() == 'l' && buf.next() == 'l' ) {
                            return Token.NULL;
                        }
                        fail( "expected null" );
                    case ':':
                        fieldNameMode = false;
                        break;
                    case '[':
                        blocks.addFirst( BlockMode.ARRAY );
                        return Token.ARRAY_OPEN;
                    case ']':
                        fieldNameMode = true;
                        blocks.poll();
                        return Token.ARRAY_CLOSE;
                    default:
                        if( Character.isDigit( c ) || c == '-' ) {
                            fieldNameMode = true;
                            return parseValue( c );
                        } else if( !isDelimiter( c ) ) fail( "unknown token " + c );
                }
            }
        }

        enum BlockMode {
            ARRAY, OBJECT
        }

        public static class Token {
            public final Type type;
            public final Object value;

            Token( Type type, Object value ) {
                this.type = type;
                this.value = value;
            }

            Token( Type type ) {
                this( type, null );
            }

            public static Token OBJECT_OPEN = new Token( Type.OBJECT_OPEN );
            public static Token OBJECT_CLOSE = new Token( Type.OBJECT_CLOSE );
            public static Token END = new Token( Type.END );
            public static Token NULL = new Token( Type.NULL );
            public static Token ARRAY_OPEN = new Token( Type.ARRAY_OPEN );
            public static Token ARRAY_CLOSE = new Token( Type.ARRAY_CLOSE );

            public static Token fieldStart( String name ) {
                return new Token( Type.FIELD_START, name );
            }

            public static Token constant( Type type, Object value ) {
                return new Token( type, value );
            }

            public enum Type {
                OBJECT_OPEN, OBJECT_CLOSE, END, FIELD_START, STRING, INTEGER, REAL, BOOL, NULL, ARRAY_OPEN, ARRAY_CLOSE
            }
        }

        /* Buffer used to parse JSON.
             * Buffer is divided to one or more segments (preallocated in Segments pool).
             */
        private static class Buffer {
            Reader in;

            Buffer( Reader in ) {
                this.in = in;
            }

            int offset = 0;
            int curMark = -1;
            int curMarkSegment = -1;
            private List<Segment> segments = new ArrayList<>( Collections.singletonList( Segments.create() ) );
            private char[] segment = segments.get( 0 ).seg;
            private int cur = 0; // Pointer which points current parsing location
            private int curSegmentIdx = 0; // Pointer which points current segment

            void mark() {
                curMark = cur;
                curMarkSegment = curSegmentIdx;
            }

            void back() {
                cur = cur - 1;
            }

            char next() {
                if( cur == offset && read() < 0 ) return EOF;
                else {
                    char c = segment[cur];
                    cur += 1;
                    return c;
                }
            }

            String substring() {
                if( curSegmentIdx == curMarkSegment ) return new String( segment, curMark, cur - curMark - 1 );
                else { // slower path for case when string is in two or more segments
                    LinkedList<Part> parts = new LinkedList<>();
                    int i = curSegmentIdx;
                    while( i >= curMarkSegment ) {
                        char[] s = segments.get( i ).seg;
                        int start = (i == curMarkSegment) ? curMark : 0;
                        int end = (i == curSegmentIdx) ? cur : s.length + 1;
                        parts.addFirst( new Part( start, end, s ) );
                        i = i - 1;
                    }
                    int len = parts.stream().map( p -> p.end - p.start - 1 ).reduce( 0, ( a, b ) -> a + b );
                    char[] chars = new char[len];
                    i = 0;
                    int pos = 0;

                    while( i < parts.size() ) {
                        Part part = parts.get( i );
                        int partLen = part.end - part.start - 1;
                        System.arraycopy( part.chars, part.start, chars, pos, partLen );
                        pos = pos + partLen;
                        i = i + 1;
                    }
                    return new String( chars );
                }
            }

            String near() {
                return new String( segment, Math.max( (cur - 20), 0 ), Math.min( (cur + 20), offset ) );
            }

            void release() {
                segments.stream().forEach( Segments::release );
            }

            private int read() {
                if( offset >= segment.length ) {
                    Segment newSegment = Segments.create();
                    offset = 0;
                    segment = newSegment.seg;
                    segments.add( newSegment );
                    curSegmentIdx = segments.size() - 1;
                }

                try {
                    int length = in.read( segment, offset, segment.length - offset );
                    cur = offset;
                    offset += length;
                    return length;
                } catch( IOException e ) {
                    throw new JsonException( e );
                }
            }
        }

        /* A pool of preallocated char arrays.
             */
        private static class Segments {
            private static int segmentSize = 1000;
            private static int maxNumOfSegments = 10000;
            private static AtomicInteger segmentCount = new AtomicInteger( 0 );
            private static ArrayBlockingQueue<Segment> segments = new ArrayBlockingQueue<>( maxNumOfSegments );

            static Segment create() {
                Segment s = acquire();
                // Give back a disposable segment if pool is exhausted.
                if( s != null ) return s;
                else return new Segment( Segment.Type.DISPOSABLE, new char[segmentSize] );
            }

            static Segment acquire() {
                int curCount = segmentCount.get();
                boolean createNew = segments.size() == 0 && curCount < maxNumOfSegments &&
                    segmentCount.compareAndSet( curCount, curCount + 1 );

                return createNew ? new Segment( Segment.Type.RECYCLED, new char[segmentSize] ) : segments.poll();
            }

            static void release( Segment s ) {
                if( s.type == Segment.Type.RECYCLED ) segments.offer( s );
            }
        }

        static class Segment {
            final Type type;
            final char[] seg;

            enum Type {
                RECYCLED, DISPOSABLE
            }

            public Segment( Type type, char[] seg ) {
                this.type = type;
                this.seg = seg;
            }
        }

        static class Part {
            final int start;
            final int end;
            final char[] chars;

            public Part( int start, int end, char[] chars ) {
                this.start = start;
                this.end = end;
                this.chars = chars;
            }
        }
    }

}
