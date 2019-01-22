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

import lombok.val;
import oap.reflect.Reflect;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 08.01.2019.
 */
public class KernelHelperTest {
    @Test
    public void testFixLinksForConstructorValueExists() {
        val parameters = new LinkedHashMap<String, Object>( Maps.of2( "service", "@service:test-service" ) );

        val si = new ServiceInitialization( "test-service", this, new Module( "n1" ), new Module.Service(), Reflect.reflect( getClass() ) );
        val newParameters = KernelHelper.fixLinksForConstructor( null, Maps.of2( "test-service", si ), parameters );

        assertThat( newParameters ).isNotSameAs( parameters );
        assertThat( newParameters ).containsKeys( "service" );
        assertThat( newParameters.get( "service" ) ).isSameAs( this );
    }

    @Test
    public void testFixLinksForConstructorValue() {
        val parameters = new LinkedHashMap<String, Object>( Maps.of2( "service", "@service:test-service" ) );

        val newParameters = KernelHelper.fixLinksForConstructor( null, new HashMap<>(), parameters );

        assertThat( newParameters ).isNotSameAs( parameters );
        assertThat( newParameters ).containsKeys( "service" );
        assertThat( newParameters.get( "service" ) ).isNull();
    }

    @Test
    public void testFixLinksForConstructorList() {
        val parameters = new LinkedHashMap<String, Object>( Maps.of2( "services", singletonList( "@service:test-service" ) ) );

        val newParameters = KernelHelper.fixLinksForConstructor( null, new HashMap<>(), parameters );

        assertThat( newParameters ).isNotSameAs( parameters );
        assertThat( newParameters ).containsKeys( "services" );
        assertThat( newParameters.get( "services" ) ).isInstanceOf( List.class );
        assertThat( ( List<Object> ) newParameters.get( "services" ) ).isEmpty();
    }

    @Test
    public void testFixLinksForConstructorMap() {
        val parameters = new LinkedHashMap<String, Object>(
            Maps.of2( "services", Lists.of( Maps.of2( "link", "@service:test-service" ) ) )
        );

        val newParameters = KernelHelper.fixLinksForConstructor( null, new HashMap<>(), parameters );

        assertThat( newParameters ).isNotSameAs( parameters );
        assertThat( newParameters ).containsKeys( "services" );
        assertThat( newParameters.get( "services" ) ).isInstanceOf( List.class );
        assertThat( ( List<Object> ) newParameters.get( "services" ) ).hasSize( 1 );
        assertThat( ( ( List<Object> ) newParameters.get( "services" ) ).get( 0 ) ).isNotNull();
        assertThat( ( Map<String, ?> ) ( ( List<Object> ) newParameters.get( "services" ) ).get( 0 ) ).isEmpty();
    }
}
