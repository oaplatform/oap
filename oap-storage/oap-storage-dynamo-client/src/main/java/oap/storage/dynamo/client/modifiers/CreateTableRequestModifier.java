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
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

import java.util.function.Consumer;

/**
 * Allows to modify CreateTableRequest before applying.
 * For instance in order to add StreamAPI:
 *  builder.streamSpecification(StreamSpecification streamSpecification = new StreamSpecification()
 *             .withStreamEnabled(true)
 *             .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));
 */
@FunctionalInterface
@API
public interface CreateTableRequestModifier extends Consumer<CreateTableRequest.Builder> {

    @Override
    void accept( CreateTableRequest.Builder builder );
}
