/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import com.google.gson.Gson;
import net.minidev.json.JSONValue;
import oap.io.Resources;
import oap.testng.AbstractPerformance;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.testng.annotations.Test;

import java.util.Map;

public class ParserPerformance extends AbstractPerformance {
    public static String yearJson = Resources.readString( ParserPerformance.class, "year.json" ).get();

    private static final JacksonJodaDateFormat jodaDateFormat = new JacksonJodaDateFormat( Dates.FORMAT_MILLIS );

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> JsonDeserializer<T> forType( Class<T> cls ) {
        return (JsonDeserializer<T>) new DateTimeDeserializer( cls, jodaDateFormat );
    }

    @Test
    public void performance() {
        benchmark( "mapParser-json-smart", 5000, 5, ( i ) -> JSONValue.toJSONString( JSONValue.parse( yearJson ) ) );

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule( new Jdk8Module() );
        final JodaModule module = new JodaModule();
        module.addDeserializer( DateTime.class, forType( DateTime.class ) );
        module.addSerializer( DateTime.class, new DateTimeSerializer( jodaDateFormat ) );
        mapper.registerModule( module );
        mapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper.registerModule( new PathModule() );

        benchmark( "mapParser-jackson", 5000, 5,
            i -> mapper.writeValueAsString( mapper.readValue( yearJson, Map.class ) ) );

        final ObjectMapper mapper2 = new ObjectMapper();
        mapper2.registerModule( new Jdk8Module() );
        mapper2.registerModule( module );
        mapper2.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper2.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper2.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper2.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper2.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper2.registerModule( new PathModule() );

        mapper2.registerModule( new AfterburnerModule() );


        benchmark( "mapParser-jackson2", 5000, 5, i ->
                mapper2.writeValueAsString( mapper2.readValue( yearJson, Map.class ) ) );

        final Gson gson = new Gson();
        benchmark( "mapParser-gson", 5000, 5, ( i ) -> gson.toJson( gson.fromJson( yearJson, Map.class ) ) );
    }
}
