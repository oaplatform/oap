/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
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

package oap.statsdb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.json.TypeIdFactory;
import oap.util.Mergeable;
import oap.util.Stream;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@EqualsAndHashCode
@ToString
@Slf4j
public class Node implements Serializable {
    private static final long serialVersionUID = 4194048067764234L;

    public volatile ConcurrentHashMap<String, Node> db = new ConcurrentHashMap<>();
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, property = "o:t" )
    public Value v;
    public long ct;
    public long mt;

    public Node() {
        this( DateTimeUtils.currentTimeMillis() );
    }

    @JsonCreator
    public Node( long ct ) {
        this.mt = this.ct = ct;
    }

    @SuppressWarnings( "unchecked" )
    synchronized <V extends Value<V>> void updateValue( Consumer<V> update, Supplier<V> create ) {
        if( v == null ) v = create.get();
        update.accept( ( V ) v );
        this.mt = DateTimeUtils.currentTimeMillis();
    }

    @SuppressWarnings( "unchecked" )
    public <V extends Value<V>> V get( Iterator<String> key ) {
        Node obj = this;

        while( key.hasNext() ) {
            val item = key.next();

            if( obj == null ) return null;

            obj = obj.db.get( item );
        }

        if( obj == null ) return null;

        return ( V ) obj.v;
    }

    @SuppressWarnings( "unchecked" )
    public boolean merge( Node node ) {
        mt = DateTimeUtils.currentTimeMillis();
        if( v == null ) v = node.v;
        else {
            try {
                if( node.v != null ) v.merge( node.v );
            } catch( Throwable t ) {
                log.error( t.getMessage(), t );

                return false;
            }
        }
        return true;
    }

    public interface Value<T extends Value<T>> extends Mergeable<T>, Serializable {
    }

    public interface Container<T extends Value<T>, TChild extends Value<TChild>> extends Value<T> {
        T aggregate( List<TChild> children );
    }
}
