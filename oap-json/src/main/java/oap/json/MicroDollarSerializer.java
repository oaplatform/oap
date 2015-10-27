package oap.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class MicroDollarSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize( Long value, JsonGenerator gen,
        SerializerProvider serializers ) throws IOException, JsonProcessingException {

        gen.writeNumber( value / 1000000d );
    }
}
