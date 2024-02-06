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

package oap.storage.dynamo.client.crud;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.exceptions.InvalidNameException;
import oap.storage.dynamo.client.restrictions.ReservedWords;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@ToString( exclude = { "putRequest", "deleteRequest" } )
public abstract class AbstractOperation {
    private final OperationType type;
    @Setter
    private DynamodbClient.State state;
    private String name;
    private final Key key;
    private final Map<String, Object> binNamesAndValues;

    @Setter
    private PutRequest putRequest;
    @Setter
    private DeleteRequest deleteRequest;

    protected AbstractOperation( OperationType type, String name ) {
        this.type = type;
        this.name = name;
        key = null;
        binNamesAndValues = Collections.emptyMap();
    }

    protected AbstractOperation( OperationType type, Key key, Map<String, Object> binNamesAndValues ) {
        this.type = type;
        this.key = key;
        this.binNamesAndValues = binNamesAndValues == null ? Collections.emptyMap() : binNamesAndValues;
        List<String> invalidBinNames = this.binNamesAndValues.keySet()
            .stream()
            .filter( o -> !ReservedWords.isAttributeNameAppropriate( o ) )
            .toList();
        if ( !invalidBinNames.isEmpty() ) {
            throw new InvalidNameException( "Bins with names '" + invalidBinNames + "' are prohibited in DynamoDB" );
        }
    }
}
