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

import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.exceptions.InvalidNameException;
import oap.storage.dynamo.client.restrictions.ReservedWords;
import org.apache.commons.codec.digest.DigestUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynamoDbHelper {

    protected String generateKeyValue( Key key ) {
        String keyValue = key.getColumnValue();
        if( keyValue.length() <= ReservedWords.MAX_NAME_LENGTH ) return keyValue;
        byte[] digest = DigestUtils.digest( DigestUtils.getDigest( "SHA-256" ), keyValue.getBytes( StandardCharsets.UTF_8 ) );
        return new String( Base64.getEncoder().encode( digest ), StandardCharsets.UTF_8 );
    }

    protected Map<String, AttributeValue> getKeyAttribute( Key key ) {
        AttributeValue keyAttribute = AttributeValue.builder().s( generateKeyValue( key ) ).build();
        return Collections.singletonMap( key.getColumnName(), keyAttribute );
    }

    protected Map<String, AttributeValue> generateBinNamesAndValues( Key key,
                                                                     String binName,
                                                                     Object binValue,
                                                                     Map<String, AttributeValue> oldValues ) {
        if ( !ReservedWords.isAttributeNameAppropriate( binName ) ) {
            throw new InvalidNameException( "Such attribute name '" + binName + "' is unsupported in DynamoDB, "
                    + "see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html" );
        }
        Map<String, AttributeValue> newValues = new HashMap<>();

        if( oldValues != null ) {
            newValues.putAll( oldValues );
        } else {
            //create an id
            newValues.putAll( getKeyAttribute( key ) );
        }
        if( binValue != null ) {
            newValues.put( binName, DynamodbDatatype.createAttributeValueFromObject( binValue ) );
        } else {
            newValues.remove( binName );
        }
        return newValues;
    }
}
