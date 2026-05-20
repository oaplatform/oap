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

import oap.dictionary.DictionaryParser;
import oap.logstream.MemoryLoggerBackend;
import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.apache.commons.lang3.mutable.MutableObject;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
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
    public void testLog() {
        String datamodel = """
            name = model
            values {
              MODEL1 {
                values {
                  a {
                    path = a
                    type = STRING
                    default = ""
                  }
                  b {
                    path = b
                    type = INTEGER
                    default = 1233
                  }
                  aaa {
                    path = a|aa
                    type = STRING
                    default = ""
                  }
                  list {
                    path = data1.list|data2.list
                    type = STRING_ARRAY
                    default = []
                  }
                  x {
                    type = INTEGER
                    default = 1
                  }
                  map_val_long_as_int {
                    path = map.map_val_long_as_int
                    type = INTEGER
                    default = 111
                  }
                }
              }
            }
            """;

        MemoryLoggerBackend memoryLoggerBackend = new MemoryLoggerBackend();
        RowBinaryObjectLogger binaryObjectLogger = new RowBinaryObjectLogger( DictionaryParser.parseFromString( datamodel ), memoryLoggerBackend, Paths.get( "/tmp/file-cache" ), Dates.d( 10 ) );

        RowBinaryObjectLogger.TypedRowBinaryLogger<TestData> logger = binaryObjectLogger.typed( new TypeRef<>() {}, "MODEL1", false );

        logger.log( new TestData( "ff", "cc", 12, List.of( "1" ), null, Map.of( "map_val_long_as_int", 333L ) ), "prefix", Map.of(), "mylog" );
        logger.log( new TestData( null, "dd", 44, null, List.of( "2" ), Map.of( "map_val_long_as_int", 1L ) ), "prefix", Map.of(), "mylog" );

        memoryLoggerBackend
            .assertRowBinary( _ -> true )
            .containsExactlyInAnyOrderEntriesOf(
                List.of( "ff", 12, "ff", List.of( "1" ), 333 ),
                List.of( "", 44, "dd", List.of( "2" ), 1 )
            );
    }

    @Test
    public void testOptimization() {
        String datamodel = """
            name = model
            values {
              MODEL1 {
                values {
                  a2 {
                    path = subData.subData.a
                    type = STRING
                    default = ""
                  }
                  a {
                    path = a
                    type = STRING
                    default = ""
                  }
                  aa2 {
                    path = subData.subData.aa
                    type = STRING
                    default = ""
                  }
                  or {
                    path = "a|aa"
                    type = STRING
                    default = ""
                  }
                  b {
                    path = b
                    type = INTEGER
                    default = 0
                  }
                  b2 {
                    path = subData.subData.b
                    type = INTEGER
                    default = 0
                  }
                }
              }
            }
            """;

        MemoryLoggerBackend memoryLoggerBackend = new MemoryLoggerBackend();
        RowBinaryObjectLogger binaryObjectLogger = new RowBinaryObjectLogger( DictionaryParser.parseFromString( datamodel ), memoryLoggerBackend, Paths.get( "/tmp/file-cache" ), Dates.d( 10 ) );

        MyTemplateEngineListener listener = new MyTemplateEngineListener();
        RowBinaryObjectLogger.TypedRowBinaryLogger<TestData> logger = binaryObjectLogger.typed( new TypeRef<>() {}, "MODEL1", true, listener );

        TestData testData = new TestData();
        testData.a = "a1";
        testData.aa = "aa1";
        testData.b = 1;

        TestData testData2 = new TestData();
        testData.subData = testData2;

        TestData testData3 = new TestData();
        testData2.subData = testData3;
        testData3.a = "a2";
        testData3.aa = "aa2";
        testData3.b = 2;

        logger.log( testData, "prefix", Map.of(), "mylog" );

        MutableObject<String[]> headers = new MutableObject<>();

        memoryLoggerBackend
            .assertRowBinary( lid -> true )
            .containOnlyHeaders( "a", "or", "b", "a2", "aa2", "b2" )
            .containsExactlyInAnyOrderEntriesOf( List.of( "a1", "a1", 1, "a2", "aa2", 2 ) );

        assertThat( listener.javaCode )
            .isEqualTo( "{{ /* model MODEL1 id a path a type STRING defaultValue '' */<java.lang.String>a ?? \"\" }}"
                + "{{ /* model MODEL1 id or path a|aa type STRING defaultValue '' */<java.lang.String>a|aa ?? \"\" }}"
                + "{{ /* model MODEL1 id b path b type INTEGER defaultValue '0' */<java.lang.Integer>b ?? 0 }}"
                + "{{% with subData.subData }}"
                + "{{ /* model MODEL1 id a2 path subData.subData.a type STRING defaultValue '' */<java.lang.String>a ?? \"\" }}"
                + "{{ /* model MODEL1 id aa2 path subData.subData.aa type STRING defaultValue '' */<java.lang.String>aa ?? \"\" }}"
                + "{{ /* model MODEL1 id b2 path subData.subData.b type INTEGER defaultValue '0' */<java.lang.Integer>b ?? 0 }}"
                + "{{% end }}" );
    }

    @Test
    public void testOptimization2() {
        String datamodel = """
            name = model
            values {
              MODEL1 {
                values {
                  a1 {
                    path = subData.a
                    type = STRING
                    default = ""
                  }
                  a2 {
                    path = subData.subData.a
                    type = STRING
                    default = ""
                  }
                  a {
                    path = a
                    type = STRING
                    default = ""
                  }
                  aa2 {
                    path = "subData.subData.{ a + 'x' + aa }"
                    type = STRING
                    default = ""
                  }
                }
              }
            }
            """;

        MemoryLoggerBackend memoryLoggerBackend = new MemoryLoggerBackend();
        RowBinaryObjectLogger binaryObjectLogger = new RowBinaryObjectLogger( DictionaryParser.parseFromString( datamodel ), memoryLoggerBackend, Paths.get( "/tmp/file-cache" ), Dates.d( 10 ) );

        MyTemplateEngineListener listener = new MyTemplateEngineListener();
        RowBinaryObjectLogger.TypedRowBinaryLogger<TestData> logger = binaryObjectLogger.typed( new TypeRef<>() {}, "MODEL1", true, listener );

        TestData testData = new TestData();
        testData.a = "a1";
        testData.aa = "aa1";
        testData.b = 1;

        TestData testData2 = new TestData();
        testData.subData = testData2;

        TestData testData3 = new TestData();
        testData2.subData = testData3;
        testData3.a = "a2";
        testData3.aa = "aa2";
        testData3.b = 2;

        logger.log( testData, "prefix", Map.of(), "mylog" );

        MutableObject<String[]> headers = new MutableObject<>();

        memoryLoggerBackend
            .assertRowBinary( lid -> true )
            .containOnlyHeaders( "a", "a1", "a2", "aa2" )
            .containsExactlyInAnyOrderEntriesOf( List.of( "a1", "", "a2", "a2xaa2" ) );

        assertThat( listener.javaCode )
            .isEqualTo( "{{ /* model MODEL1 id a path a type STRING defaultValue '' */<java.lang.String>a ?? \"\" }}"
                + "{{% with subData }}"
                + "{{ /* model MODEL1 id a1 path subData.a type STRING defaultValue '' */<java.lang.String>a ?? \"\" }}"
                + "{{% with subData }}"
                + "{{ /* model MODEL1 id a2 path subData.subData.a type STRING defaultValue '' */<java.lang.String>a ?? \"\" }}"
                + "{{ /* model MODEL1 id aa2 path subData.subData.{ a + 'x' + aa } type STRING defaultValue '' */<java.lang.String>a + 'x' + aa ?? \"\" }}"
                + "{{% end }}"
                + "{{% end }}" );
    }

    public static class TestData {
        public final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        public String a;
        public String aa;
        public int b;
        public Optional<TestData1> data1 = Optional.empty();
        public Optional<TestData1> data2 = Optional.empty();
        @Nullable
        public TestData subData;

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

    private static class MyTemplateEngineListener implements RowBinaryObjectLogger.RowBinaryObjectListener {
        private String javaCode;

        @Override
        public void javaCode( String javaCode ) {
            this.javaCode = javaCode;
        }
    }
}
