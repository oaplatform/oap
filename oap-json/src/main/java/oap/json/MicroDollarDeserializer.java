package oap.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class MicroDollarDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {
        return (long) (p.getDoubleValue() * 1000000);
    }
}
