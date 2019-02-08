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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import oap.util.Stream;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@Slf4j
public class JsonPatch {
    public <T> T patchObject( Object dest, String draftJson ) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        Gson gson = new Gson();

        Map<String, Object> destSchema = mapper.convertValue( dest, new TypeReference<Map<String, Object>>() {} );
        Map<String, Object> sourceSchema;
        try {
            sourceSchema = mapper.readValue( draftJson, new TypeReference<Map<String, Object>>() {} );

            Map<String, Object> finalSourceSchema = sourceSchema;
            Stream.of( destSchema.entrySet() ).filter( e -> !e.getKey().equals( "id" ) )
                .filter( e -> finalSourceSchema.containsKey( e.getKey() ) )
                .forEach( e -> e.setValue( finalSourceSchema.get( e.getKey() ) ) );
        } catch( IOException e ) {
            log.error( "Something went wrong ", e );
            return ( T ) dest;
        }

        String json = gson.toJson( destSchema );
        return gson.fromJson( json, ( Type ) dest.getClass() );
    }
}
