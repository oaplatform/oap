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
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import oap.io.Resources;
import oap.json.Binder;
import oap.json.OapJsonModule;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;

import static oap.benchmark.Benchmark.benchmark;
import static oap.io.content.ContentReader.ofString;

public class ParserPerformance {
    private static final JacksonJodaDateFormat jodaDateFormat = new JacksonJodaDateFormat( Dates.FORMAT_MILLIS );
    public static String yearJson = Resources.read( ParserPerformance.class, "year.json", ofString() ).orElseThrow();

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> JsonDeserializer<T> forType( Class<T> cls ) {
        return ( JsonDeserializer<T> ) new DateTimeDeserializer( cls, jodaDateFormat );
    }

    @Test
    public void performance() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule( new Jdk8Module() );
        final JodaModule module = new JodaModule();
        module.addDeserializer( DateTime.class, forType( DateTime.class ) );
        module.addSerializer( DateTime.class, new DateTimeSerializer( jodaDateFormat, 0 ) );
        mapper.registerModule( module );
        mapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper.registerModule( new OapJsonModule() );

        benchmark( "mapParser-jackson", 5000,
            () -> mapper.writeValueAsString( mapper.readValue( yearJson, Map.class ) ) ).run();

        final ObjectMapper mapper2 = new ObjectMapper();
        mapper2.registerModule( new Jdk8Module() );
        mapper2.registerModule( module );
        mapper2.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper2.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper2.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper2.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper2.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper2.registerModule( new OapJsonModule() );

        mapper2.registerModule( new AfterburnerModule() );


        benchmark( "mapParser-jackson2", 5000,
            () -> mapper2.writeValueAsString( mapper2.readValue( yearJson, Map.class ) ) ).run();

    }

    @Test
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
