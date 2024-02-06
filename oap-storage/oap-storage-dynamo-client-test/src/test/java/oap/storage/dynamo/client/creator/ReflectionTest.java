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

package oap.storage.dynamo.client.creator;

import oap.storage.dynamo.client.creator.samples.FinalFieldInArgsConstructor;
import oap.storage.dynamo.client.creator.samples.NoDefaultConstructor;
import oap.storage.dynamo.client.creator.samples.NoPublicConstructor;
import oap.storage.dynamo.client.creator.samples.SeveralDifferentFinalFieldInArgsConstructor;
import oap.storage.dynamo.client.creator.samples.SeveralFinalFieldInArgsConstructor;
import oap.testng.Fixtures;
import oap.util.Maps;
import oap.util.Pair;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public class ReflectionTest extends Fixtures {

    @Test
    public void missingPublicConstructor() {
        PojoBeanFromDynamoCreator<NoPublicConstructor> creator = new PojoBeanFromDynamoCreator<>();
        assertThrows( ReflectiveOperationException.class,
                () -> creator.createInstanceOfBeanClass( NoPublicConstructor.class, Collections.emptyMap() ) );
    }

    @Test
    public void missingNoArgsConstructor() {
        PojoBeanFromDynamoCreator<NoDefaultConstructor> creator = new PojoBeanFromDynamoCreator<>();
        assertThrows( ReflectiveOperationException.class,
                () -> creator.createInstanceOfBeanClass( NoDefaultConstructor.class, Collections.emptyMap() ) );
    }

    @Test
    public void existsOnlyFinalArgConstructor() throws Exception {
        PojoBeanFromDynamoCreator<FinalFieldInArgsConstructor> creator = new PojoBeanFromDynamoCreator<>();
        FinalFieldInArgsConstructor instance = creator.createInstanceOfBeanClass( FinalFieldInArgsConstructor.class,
                Collections.singletonMap( "finalField", AttributeValue.fromS( "value" ) ) );

        assertNotNull( instance );
        assertEquals( "value", instance.getFinalField() );
    }

    @Test
    public void existsOnlyFinalArgsConstructor() throws Exception {
        PojoBeanFromDynamoCreator<SeveralFinalFieldInArgsConstructor> creator = new PojoBeanFromDynamoCreator<>();
        SeveralFinalFieldInArgsConstructor instance = creator.createInstanceOfBeanClass( SeveralFinalFieldInArgsConstructor.class,
                Maps.of(
                    new Pair<>( "finalField1", AttributeValue.fromS( "value1" ) ),
                    new Pair<>( "finalField2", AttributeValue.fromS( "value2" ) ),
                    new Pair<>( "finalField3", AttributeValue.fromS( "value3" ) )
                )
        );

        assertNotNull( instance );
        assertEquals( "value1", instance.getFinalField1() );
        assertEquals( "value2", instance.getFinalField2() );
        assertEquals( "value3", instance.getFinalField3() );
    }

    @Test
    public void existsOnlyFinalDifferentTypesPrimitivesArgsConstructor() throws Exception {
        PojoBeanFromDynamoCreator<SeveralDifferentFinalFieldInArgsConstructor> creator = new PojoBeanFromDynamoCreator<>();
        SeveralDifferentFinalFieldInArgsConstructor instance = creator.createInstanceOfBeanClass( SeveralDifferentFinalFieldInArgsConstructor.class,
                Maps.of(
                        new Pair<>( "finalField1", AttributeValue.fromS( "value1" ) ),
                        new Pair<>( "finalField2", AttributeValue.fromBool( true ) ),
                        new Pair<>( "finalField3", AttributeValue.fromN( "123456789" ) )
                )
        );

        assertNotNull( instance );
        assertEquals( "value1", instance.getFinalField1() );
        assertEquals( true, instance.isFinalField2() );
        assertEquals( 123456789, instance.getFinalField3().intValue() );
    }
}
