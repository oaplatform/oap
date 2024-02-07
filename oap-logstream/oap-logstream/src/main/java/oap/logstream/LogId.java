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

package oap.logstream;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


@ToString
@EqualsAndHashCode( exclude = "clientHostname" )
@Slf4j
public class LogId implements Serializable {
    @Serial
    private static final long serialVersionUID = -6026646143366760882L;

    public final String logType;
    public final String clientHostname;
    public final byte[][] types;
    public final String[] headers;

    public final String filePrefixPattern;
    public final LinkedHashMap<String, String> properties = new LinkedHashMap<>();

    public LogId( String filePrefixPattern, String logType, String clientHostname,
                  Map<String, String> properties,
                  String[] headers,
                  byte[][] types ) {
        this.filePrefixPattern = filePrefixPattern;
        this.logType = logType;
        this.clientHostname = clientHostname;
        this.types = types;
        this.properties.putAll( properties );
        this.headers = headers;

        assert headers.length == types.length : "headers " + Arrays.deepToString( headers ) + " types " + Arrays.deepToString( types );
    }

    public int getHash() {
        Hasher hasher = Hashing.murmur3_32_fixed().newHasher();

        for( var header : headers ) hasher.putString( header, UTF_8 );
        for( var type : types ) hasher.putBytes( type );

        return hasher.hash().asInt();
    }

    public final String lock() {
        return ( String.join( "-", properties.values() )
            + String.join( "-", List.of(
            filePrefixPattern,
            logType,
            Arrays.deepToString( headers ),
            Arrays.deepToString( types )
        ) ) ).intern();
    }
}
