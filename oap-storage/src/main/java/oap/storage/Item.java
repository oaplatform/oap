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

package oap.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.TypeIdFactory;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;

@EqualsAndHashCode( exclude = { "object", "metadata" } )
@ToString( exclude = "object" )
public class Item<T> implements Serializable {
    public long modified = DateTimeUtils.currentTimeMillis();
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "object:type" )
    public T object;

    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "metadata:type" )
    @JsonInclude( JsonInclude.Include.NON_NULL )
    public Object metadata;

    @JsonCreator
    protected Item( T object, Object metadata ) {
        this.object = object;
        this.metadata = metadata;
    }

    protected Item() {
    }

    public void update( T t ) {
        update( t, null );
    }

    public void update( T t, Object metadata ) {
        this.object = t;
        if( metadata != null ) this.metadata = metadata;
        setUpdated();
    }

    public void setUpdated() {
        this.modified = DateTimeUtils.currentTimeMillis();
    }
}
