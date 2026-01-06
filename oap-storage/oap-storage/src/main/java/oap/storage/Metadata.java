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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import oap.json.TypeIdFactory;
import org.joda.time.DateTimeUtils;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
public class Metadata<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -4330659282261369252L;

    public long modified = DateTimeUtils.currentTimeMillis();
    public long created = DateTimeUtils.currentTimeMillis();
    public String createdBy;
    public String modifiedBy;
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "object:type" )
    public T object;

    @JsonCreator
    protected Metadata( T object, String createdBy ) {
        update( object, createdBy );
    }

    protected Metadata() {
    }

    public static <T> Metadata<T> from( Metadata<T> metadata ) {
        Metadata<T> m = new Metadata<>( metadata.object, metadata.modifiedBy );
        m.modified = metadata.modified;
        m.createdBy = metadata.createdBy;
        m.modifiedBy = metadata.modifiedBy;
        m.created = metadata.created;
        return m;
    }

    public Metadata<T> update( T t, String modifiedBy ) {
        this.object = t;
        if( this.createdBy == null ) {
            this.createdBy = modifiedBy;
        }
        this.modifiedBy = modifiedBy;
        refresh();
        return this;
    }

    public void refresh() {
        this.modified = DateTimeUtils.currentTimeMillis();
    }

    public boolean looksUnmodified( Metadata<T> metadata ) {
        return modified == metadata.modified;
    }

    @Override
    public String toString() {
        return "Metadata("
            + "created=" + created
            + ", modified=" + modified
            + ", object=" + object
            + ')';
    }
}
