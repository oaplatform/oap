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

package oap.dictionary;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 29.04.2016.
 */
public interface Dictionary extends Cloneable {
    int getOrDefault( String id, int defaultValue );

    Integer get( String id );

    String getOrDefault( int externlId, String defaultValue );

    boolean containsValueWithId( String id );

    List<String> ids();

    int[] externalIds();

    Map<String, Object> getProperties();

    Optional<? extends Dictionary> getValueOpt( String name );

    Dictionary getValue( String name );

    Dictionary getValue( int externalId );

    List<? extends Dictionary> getValues();

    default List<Dictionary> getValues( Predicate<Dictionary> p ) {
        return getValues().stream().filter( p ).collect( toList() );
    }

    String getId();

    <T> Optional<T> getProperty( String name );

    default <T> T getPropertyOrThrow( String name ) {
        return this.<T>getProperty( name )
            .orElseThrow( () -> new IllegalArgumentException( getId() + ": not found" ) );
    }

    boolean isEnabled();

    int getExternalId();

    boolean containsProperty( String name );

    @SuppressWarnings( "unchecked" )
    default List<String> getTags() {
        return ( List<String> ) getProperty( "tags" ).orElse( Collections.emptyList() );
    }

    Dictionary clone();
}
