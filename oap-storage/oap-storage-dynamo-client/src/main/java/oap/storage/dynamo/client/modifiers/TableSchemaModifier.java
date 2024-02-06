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

package oap.storage.dynamo.client.modifiers;

import oap.storage.dynamo.client.annotations.API;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

import java.util.function.Consumer;

/**
 * Simple attribute example:
 *      builder.addAttribute(String.class, a -> a.name("attribute")
 *                         .getter(T::getAttribute)
 *                         .setter(T::setAttribute))
 *
 * Secondary partition key attribute example:
 *      builder.addAttribute(String.class, a -> a.name("attribute2*")
 *                         .getter(T::getAttribute2)
 *                         .setter(T::setAttribute2)
 *                         .tags(secondaryPartitionKey("gsi_1")))
 *
 * Secondary sort key attribute example:
 *      builder.addAttribute(String.class, a -> a.name(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
 *                         .getter(T::getAttribute3)
 *                         .setter(T::setAttribute3)
 *                         .tags(secondarySortKey("gsi_1")))
 *
 *
 *  Where T type is extending type for a Record type.
 * @param <T>
 */
@FunctionalInterface
@API
public interface TableSchemaModifier<T> extends Consumer<StaticTableSchema.Builder<T>> {

    @Override
    void accept( StaticTableSchema.Builder<T> builder );
}
