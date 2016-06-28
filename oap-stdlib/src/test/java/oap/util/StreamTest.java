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

package oap.util;

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.util.*;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class StreamTest extends AbstractTest {
    @Test
    public void traverse() {
        assertEquals(
            Stream.<Class>traverse( ArrayList.class, Class::getSuperclass ).toList(),
            Lists.of( ArrayList.class, AbstractList.class, AbstractCollection.class, Object.class ) );
    }

    @Test
    public void zipWithIndex() {
        assertEquals(
            Stream.of( "1", "2", "3" ).zipWithIndex().toList(),
            Lists.of( __( "1", 0 ), __( "2", 1 ), __( "3", 2 ) ) );
    }

    @Test
    public void partition() {
        Pair<Stream<Integer>, Stream<Integer>> streams = Stream.of( 1, 2, 3, 4, 5 ).partition( x -> x % 2 == 0 );
        assertEquals( streams._1.toList(), Lists.of( 2, 4 ) );
        assertEquals( streams._2.toList(), Lists.of( 1, 3, 5 ) );
    }

    @Test
    public void testFlatMapOptional() {
        Stream<Integer> integerStream = Stream.of( 1, null, 2, null, null, 3 );
        List<Integer> flatten = integerStream.flatMapOptional( Optional::ofNullable ).toList();
        assertEquals( flatten, Lists.of( 1, 2, 3 ) );
    }

    @Test
    public void testDistinctByProperty() {
        LinkedHashMap<String, String> kievUA = Maps.of( __( "name", "kiev" ), __( "localized", "Kyiv" ) );
        LinkedHashMap<String, String> kievRU = Maps.of( __( "name", "kiev" ), __( "localized", "Kiev" ) );
        LinkedHashMap<String, String> odessa = Maps.of( __( "name", "odessa" ), __( "localized", "Odessa" ) );
        LinkedHashMap<String, String> odessaDupe = Maps.of( __( "name", "odessa" ), __( "localized", "Odessa" ) );
        LinkedHashMap<String, String> kharkiv = Maps.of( __( "name", "kharkiv" ), __( "localized", "Kharkiv" ) );

        Stream<LinkedHashMap<String, String>> cities = Stream.of( kievRU, kievUA, odessa, odessaDupe, kharkiv );
        List<LinkedHashMap<String, String>> distinctCities = cities
            .distinctByProperty( city -> city.get( "name" ) )
            .toList();

        assertEquals( distinctCities.size(), 3 );
        assertTrue( distinctCities.contains( kievRU ) ); //distinct preserves order, so first entry should be kept
        assertFalse( distinctCities.contains( kievUA ) ); //distinct preserves order, second entry is thrown out

        assertTrue( distinctCities.contains( odessa ) );
        assertTrue( distinctCities.contains( kharkiv ) );
    }
}
