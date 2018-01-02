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
package oap.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@ToString( exclude = "listener" )
@EqualsAndHashCode
public class ServiceOne {
    static volatile int instances;
    int i;
    int i2;
    Complex complex;
    List<Complex> complexes = new ArrayList<>();
    Map<String, ServiceOne> map;
    ComplexMap complexMap;
    ActionListener listener;
    List<ServiceOne> list = new ArrayList<>();
    List<TestRec> list2 = new ArrayList<>();

    public ServiceOne( int i ) {
        this.i = i;
    }

    public void addSomeListener( ActionListener listener ) {
        this.listener = listener;
    }

    public static class TestRec<T extends TestRec> {
        public ArrayList<T> recs = new ArrayList<>();
    }

    @EqualsAndHashCode
    @ToString
    public static class Complex {
        int i;
        Map<String, Complex> map = new HashMap<>();

        @JsonCreator
        public Complex( @JsonProperty( "i" ) int i ) {
            this.i = i;
        }
    }

    @ToString
    @Slf4j
    public static class ComplexMap implements Map<String, Complex> {

        public ComplexMap() {
            log.info( "init()" );
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey( Object key ) {
            return false;
        }

        @Override
        public boolean containsValue( Object value ) {
            return false;
        }

        @Override
        public Complex get( Object key ) {
            return null;
        }

        @Override
        public Complex put( String key, Complex value ) {
            return null;
        }

        @Override
        public Complex remove( Object key ) {
            return null;
        }

        @Override
        public void putAll( Map<? extends String, ? extends Complex> m ) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Set<String> keySet() {
            return emptySet();
        }

        @Override
        public Collection<Complex> values() {
            return emptyList();
        }

        @Override
        public Set<Entry<String, Complex>> entrySet() {
            return emptySet();
        }
    }
}
