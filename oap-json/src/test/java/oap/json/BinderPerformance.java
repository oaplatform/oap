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

package oap.json;

import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_INT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;

/**
 * Created by igor.petrenko on 06.12.2016.
 */
@Test( enabled = false )
public class BinderPerformance extends AbstractPerformance {
    @Test( enabled = false )
    public void testArrayVsList() {
        final String source = "{\"test\":[\"1\",\"2\",\"3\"],\"test2\":[1,2,3]}";

        final TArray tArray = new TArray( new String[] { "1", "2", "3" }, new int[] { 1, 2, 3 } );
        final TList tList = new TList( Arrays.asList( "1", "2", "3" ), Arrays.asList( 1, 2, 3 ) );
        final TMix tMix = new TMix( Arrays.asList( "1", "2", "3" ), new int[] { 1, 2, 3 } );

        final int samples = 10000000;

        benchmark( "arraylist-deserialization", samples, () -> Binder.json.unmarshal( TMix.class, source ) ).run();
        benchmark( "array-deserialization", samples, () -> Binder.json.unmarshal( TArray.class, source ) ).run();
        benchmark( "list-deserialization", samples, () -> Binder.json.unmarshal( TList.class, source ) ).run();

        benchmark( "array-serialization", samples, () -> Binder.json.marshal( tArray ) ).run();
        benchmark( "arraylist-serialization", samples, () -> Binder.json.marshal( tMix ) ).run();
        benchmark( "list-serialization", samples, () -> Binder.json.marshal( tList ) ).run();
    }

    public static class TArray {
        public String[] test = EMPTY_STRING_ARRAY;
        public int[] test2 = EMPTY_INT_ARRAY;

        public TArray( String[] test, int[] test2 ) {
            this.test = test;
            this.test2 = test2;
        }
    }

    public static class TList {
        public List<String> test = new ArrayList<>();
        public List<Integer> test2 = new ArrayList<>();

        public TList( List<String> test, List<Integer> test2 ) {
            this.test = test;
            this.test2 = test2;
        }
    }

    public static class TMix {
        public List<String> test = new ArrayList<>();
        public int[] test2 = EMPTY_INT_ARRAY;

        public TMix( List<String> test, int[] test2 ) {
            this.test = test;
            this.test2 = test2;
        }
    }
}
