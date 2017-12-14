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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by igor.petrenko on 14.12.2017.
 */
class DictionaryExtends implements Dictionary {
    final Extends anExtends;

    public DictionaryExtends( Extends anExtends ) {this.anExtends = anExtends;}

    @Override
    public int getOrDefault( String id, int defaultValue ) {
        throw new IllegalAccessError();
    }

    @Override
    public Integer get( String id ) {
        throw new IllegalAccessError();
    }

    @Override
    public String getOrDefault( int externlId, String defaultValue ) {
        throw new IllegalAccessError();
    }

    @Override
    public boolean containsValueWithId( String id ) {
        throw new IllegalAccessError();
    }

    @Override
    public List<String> ids() {
        throw new IllegalAccessError();
    }

    @Override
    public int[] externalIds() {
        throw new IllegalAccessError();
    }

    @Override
    public Map<String, Object> getProperties() {
        throw new IllegalAccessError();
    }

    @Override
    public Optional<? extends Dictionary> getValueOpt( String name ) {
        throw new IllegalAccessError();
    }

    @Override
    public Dictionary getValue( String name ) {
        throw new IllegalAccessError();
    }

    @Override
    public Dictionary getValue( int externalId ) {
        throw new IllegalAccessError();
    }

    @Override
    public List<? extends Dictionary> getValues() {
        throw new IllegalAccessError();
    }

    @Override
    public String getId() {
        throw new IllegalAccessError();
    }

    @Override
    public <T> Optional<T> getProperty( String name ) {
        throw new IllegalAccessError();
    }

    @Override
    public boolean isEnabled() {
        throw new IllegalAccessError();
    }

    @Override
    public int getExternalId() {
        throw new IllegalAccessError();
    }

    @Override
    public boolean containsProperty( String name ) {
        throw new IllegalAccessError();
    }
}
