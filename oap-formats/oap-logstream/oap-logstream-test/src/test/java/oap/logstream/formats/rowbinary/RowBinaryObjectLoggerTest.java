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

package oap.logstream.formats.rowbinary;

import oap.dictionary.DictionaryLeaf;
import oap.dictionary.DictionaryRoot;
import oap.dictionary.DictionaryValue;
import oap.logstream.MemoryLoggerBackend;
import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RowBinaryObjectLoggerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public RowBinaryObjectLoggerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void testLog() throws IOException {
        MemoryLoggerBackend memoryLoggerBackend = new MemoryLoggerBackend();
        RowBinaryObjectLogger binaryObjectLogger = new RowBinaryObjectLogger( new DictionaryRoot( "model", List.of(
            new DictionaryValue( "MODEL1", true, 1, List.of(
                new DictionaryLeaf( "a", true, 2, Map.of( "path", "a", "type", "STRING", "default", "" ) ),
                new DictionaryLeaf( "b", true, 2, Map.of( "path", "b", "type", "INTEGER", "default", 123 ) ),
                new DictionaryLeaf( "aaa", true, 2, Map.of( "path", "a | default aa", "type", "STRING", "default", "" ) ),
                new DictionaryLeaf( "list", true, 2, Map.of( "path", "data1.list | default data2.list", "type", "STRING_ARRAY", "default", "[]" ) ),
                new DictionaryLeaf( "x", true, 2, Map.of( "type", "INTEGER", "default", 1 ) ),
                new DictionaryLeaf( "map_val_long_as_int", true, 2, Map.of( "path", "map.map_val_long_as_int", "type", "INTEGER", "default", 111 ) )
            ) )
        ) ), memoryLoggerBackend, Paths.get( "/tmp/file-cache" ), Dates.d( 10 ) );

        RowBinaryObjectLogger.TypedRowBinaryLogger<TestData> logger = binaryObjectLogger.typed( new TypeRef<>() {}, "MODEL1" );

        logger.log( new TestData( "ff", "cc", 12, List.of( "1" ), null, Map.of( "map_val_long_as_int", 333L ) ), "prefix", Map.of(), "mylog" );
        logger.log( new TestData( null, "dd", 44, null, List.of( "2" ), Map.of( "map_val_long_as_int", 1L ) ), "prefix", Map.of(), "mylog" );

        List<List<Object>> bytes = memoryLoggerBackend.asRowBinary( _ -> true );

        assertThat( bytes ).isEqualTo( List.of(
            List.of( "ff", 12, "ff", List.of( "1" ), 333 ),
            List.of( "", 44, "dd", List.of( "2" ), 1 )
        ) );
    }

    public static class TestData {
        public final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        public String a;
        public String aa;
        public int b;
        public Optional<TestData1> data1 = Optional.empty();
        public Optional<TestData1> data2 = Optional.empty();

        public TestData() {
        }

        public TestData( String a, String aa, int b, List<String> data1, List<String> data2, Map<String, Object> map ) {
            this.a = a;
            this.aa = aa;
            this.b = b;

            if( data1 != null ) this.data1 = Optional.of( new TestData1( data1 ) );
            if( data2 != null ) this.data2 = Optional.of( new TestData1( data2 ) );

            this.map.putAll( map );
        }

        public static class TestData1 {
            public final ArrayList<String> list = new ArrayList<>();

            public TestData1( List<String> list ) {
                this.list.addAll( list );
            }
        }
    }
}
