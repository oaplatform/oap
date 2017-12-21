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

package oap.storage;

import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import lombok.val;
import org.bson.codecs.configuration.CodecRegistries;

import java.io.Closeable;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
public class MongoClient implements Closeable {
    protected final com.mongodb.MongoClient mongoClient;
    private final String host;
    private final int port;

    public MongoClient( String host, int port ) {
        this.host = host;
        this.port = port;

        val codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JodaTimeCodec() ),
            com.mongodb.MongoClient.getDefaultCodecRegistry() );

        val options = MongoClientOptions.builder().codecRegistry( codecRegistry ).build();

        mongoClient = new com.mongodb.MongoClient( new ServerAddress( host, port ), options );
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    public MongoDatabase getDatabase( String database ) {
        return mongoClient.getDatabase( database );
    }
}
