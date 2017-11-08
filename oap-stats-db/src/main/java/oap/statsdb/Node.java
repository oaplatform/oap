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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.json.TypeIdFactory;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@EqualsAndHashCode
@ToString
@Slf4j
public class Node implements Serializable {
    private static final long serialVersionUID = 4194048067764234L;

    public final String name;
    volatile ConcurrentHashMap<String, Node> db = new ConcurrentHashMap<>();
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "object:type" )
    Value value;
    long createdTime;
    long modifiedTime;

    @JsonCreator
    public Node( @JsonProperty String name ) {
        this( name, DateTimeUtils.currentTimeMillis() );
    }

    public Node( String name, long createdTime ) {
        this.name = name;
        this.modifiedTime = this.createdTime = createdTime;
    }

    @SuppressWarnings( "unchecked" )
    synchronized <TValue extends Value<TValue>> void updateValue( Consumer<TValue> update, Supplier<TValue> create ) {
        if( value == null ) value = create.get();
        update.accept( ( TValue ) value );
        this.modifiedTime = DateTimeUtils.currentTimeMillis();
    }

    @SuppressWarnings( "unchecked" )
    public <TValue extends Value<TValue>> TValue get( Iterator<String> key ) {
        Node obj = this;

        while( key.hasNext() ) {
            val item = key.next();

            if( obj == null ) return null;

            obj = obj.db.get( item );
        }

        if( obj == null ) return null;

        return ( TValue ) obj.value;
    }

    @SuppressWarnings( "unchecked" )
    public boolean merge( Node node ) {
        modifiedTime = DateTimeUtils.currentTimeMillis();
        if( value == null ) value = node.value;
        else {
            try {
                if( value instanceof Container<?, ?> )
                    ( ( Container ) value ).aggregate( db.values().stream().map( n -> n.value ) );
                value.merge( node.value );
            } catch( Throwable t ) {
                log.error( t.getMessage(), t );

                return false;
            }
        }
        return true;
    }

    public interface Value<T extends Value<T>> extends Serializable {
        T merge( T other );
    }

    public interface Container<T extends Value<T>, TChild extends Value<TChild>> extends Value<T> {
        T aggregate( Stream<TChild> children );
    }
}
