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

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;

/**
 * Created by igor.petrenko on 05.01.2018.
 */
public class UniqueField<T> implements Storage.Constraint<T> {
    private final String type;
    private final Function<T, Object> valueFunc;

    public UniqueField( String type, Function<T, Object> valueFunc ) {
        this.type = type;
        this.valueFunc = valueFunc;
    }

    @Override
    public void check( T object, Storage<T> storage, Function<T, String> id ) throws ConstraintException {
        val idValue = id.apply( object );
        val value = valueFunc.apply( object );

        if( storage
            .select()
            .anyMatch( obj -> Objects.equals( value, valueFunc.apply( obj ) ) && !idValue.equals( id.apply( obj ) ) ) ) {
            throw new ConstraintException( StringUtils.capitalize( type ) + " '" + value + "' already exists." );
        }
    }
}
