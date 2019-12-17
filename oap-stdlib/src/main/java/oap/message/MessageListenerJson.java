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

package oap.message;

import oap.json.Binder;
import oap.reflect.TypeRef;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public abstract class MessageListenerJson<T> implements MessageListener {
    private final byte messageType;
    private final String info;
    private final TypeRef<T> typeRef;

    public MessageListenerJson( byte messageType, String info, TypeRef<T> typeRef ) {
        this.messageType = messageType;
        this.info = info;
        this.typeRef = typeRef;
    }

    @Override
    public byte getId() {
        return messageType;
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void run( int version, String hostName, int size, byte[] data ) {
        var obj = Binder.json.unmarshal( typeRef, data );
        run( version, hostName, obj );
    }

    protected abstract void run( int version, String hostName, T data );
}
