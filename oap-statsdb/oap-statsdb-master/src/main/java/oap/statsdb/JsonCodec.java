package oap.statsdb;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.SneakyThrows;
import oap.json.Binder;
import oap.reflect.TypeRef;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

/**
 * Created by igor.petrenko on 26.03.2019.
 */
class JsonNodeCodec implements Codec<MongoNode> {
    private final DocumentCodec documentCodec;
    private ObjectWriter fileWriter;
    private ObjectReader fileReader;

    JsonNodeCodec() {
        this.documentCodec = new DocumentCodec();
        var ref = new TypeRef<MongoNode>() {
        };
        this.fileReader = Binder.json.readerFor( ref );
        this.fileWriter = Binder.json.writerFor( ref );
    }

    @SneakyThrows
    @Override
    public MongoNode decode( BsonReader bsonReader, DecoderContext decoderContext ) {
        var doc = documentCodec.decode( bsonReader, decoderContext );

        return fileReader.readValue( Binder.json.marshal( doc ) );
    }

    @SneakyThrows
    @Override
    public void encode( BsonWriter bsonWriter, MongoNode data, EncoderContext encoderContext ) {
        var doc = Document.parse( fileWriter.writeValueAsString( data ) );

        documentCodec.encode( bsonWriter, doc, encoderContext );
    }

    @Override
    public Class<MongoNode> getEncoderClass() {
        return MongoNode.class;
    }
}
