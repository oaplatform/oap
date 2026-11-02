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
package oap.benchmark;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import oap.io.Resources;
import oap.json.Binder;
import oap.json.OapJsonModule;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.testng.annotations.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.datatype.joda.JodaModule;
import tools.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import tools.jackson.datatype.joda.deser.DateTimeDeserializer;
import tools.jackson.datatype.joda.ser.DateTimeSerializer;
import tools.jackson.module.afterburner.AfterburnerModule;

import java.util.Map;
import java.util.Optional;

import static oap.benchmark.Benchmark.benchmark;
import static oap.io.content.ContentReader.ofString;

@Test( enabled = false )
public class ParserPerformance {
    private static final JacksonJodaDateFormat jodaDateFormat = new JacksonJodaDateFormat( Dates.FORMAT_MILLIS );
    public static String yearJson = Resources.read( ParserPerformance.class, "/oap/json/year.json", ofString() ).orElseThrow();

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> ValueDeserializer<T> forType( Class<T> cls ) {
        return ( ValueDeserializer<T> ) new DateTimeDeserializer( cls, jodaDateFormat );
    }

    @Test( enabled = false )
    public void performance() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer( DateTime.class, forType( DateTime.class ) );
        module.addSerializer( DateTime.class, new DateTimeSerializer( jodaDateFormat, 0 ) );
        final var builder = JsonMapper.builder()
            .addModule( new JodaModule() )
            .addModule( module )
            .enable( DeserializationFeature.USE_LONG_FOR_INTS )
            .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES )
            .disable( DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS )
            .changeDefaultVisibility( v -> v.withFieldVisibility( JsonAutoDetect.Visibility.ANY ) )
            .changeDefaultPropertyInclusion( v -> JsonInclude.Value.construct( JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS ) )
            .addModule( new OapJsonModule() );
        final ObjectMapper mapper = builder.build();

        benchmark( "mapParser-jackson", 5000,
            () -> mapper.writeValueAsString( mapper.readValue( yearJson, Map.class ) ) ).run();

        final ObjectMapper mapper2 = builder.addModule( new AfterburnerModule() ).build();

        benchmark( "mapParser-jackson2", 5000,
            () -> mapper2.writeValueAsString( mapper2.readValue( yearJson, Map.class ) ) ).run();

    }

    @Test( enabled = false )
    public void testNullVsOptional() {
        final String testEmpty = Binder.json.marshal( new TestNull( null, null, null ) );
        final String testNotEmpty = Binder.json.marshal( new TestNull( "123", "567", new TestNull( "q", "w", new TestNull( null, null, null ) ) ) );

        System.out.println( testEmpty );
        System.out.println( testNotEmpty );

        benchmark( "parse-null", 5000000, () -> {
            Binder.json.unmarshal( TestNull.class, testEmpty );
            Binder.json.unmarshal( TestNull.class, testNotEmpty );
        } ).run();
        benchmark( "parse-optional-empty", 5000000, () -> {
            Binder.json.unmarshal( TestOptional.class, testEmpty );
            Binder.json.unmarshal( TestOptional.class, testNotEmpty );
        } ).run();
    }

    public static class TestNull {
        public String test1;
        public String test2;
        public TestNull test3;

        public TestNull( String test1, String test2, TestNull test3 ) {
            this.test1 = test1;
            this.test2 = test2;
            this.test3 = test3;
        }

        public TestNull() {
        }
    }

    public static class TestOptional {
        public Optional<String> test1;
        public Optional<String> test2;
        public Optional<TestOptional> test3;

        public TestOptional( Optional<String> test1, Optional<String> test2, Optional<TestOptional> test3 ) {
            this.test1 = test1;
            this.test2 = test2;
            this.test3 = test3;
        }

        public TestOptional() {
        }
    }
}
