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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.TypeIdFactory;
import org.joda.time.DateTimeUtils;

import java.util.UUID;

@EqualsAndHashCode( exclude = "object" )
@ToString( exclude = "object" )
public class Metadata<T> implements Comparable<Metadata<T>> {
    public int version;
    public String id;
    public long modified = DateTimeUtils.currentTimeMillis();
    public boolean deleted;
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "object:type" )
    public T object;
    public String uniqueId = UUID.randomUUID().toString();

    public Metadata( String id, T object, int version ) {
        this.id = id;
        this.object = object;
        this.version = version;
    }

    public Metadata() {
    }

    @Override
    public int compareTo( Metadata<T> o ) {
        return this.id.compareTo( o.id );
    }

    public void update( T t ) {
        this.object = t;
        this.modified = DateTimeUtils.currentTimeMillis();
    }

    public void delete() {
        this.deleted = true;
        this.modified = DateTimeUtils.currentTimeMillis();
    }
}
