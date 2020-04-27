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

package oap.application.remote;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * Created by igor.petrenko on 2020-04-27.
 */
public interface Remotes {
    int VERSION = 2;

    Pool<Kryo> kryoPool = new Pool<>( true, false, 8 ) {
        protected Kryo create() {
            var kryo = new Kryo();
            kryo.setRegistrationRequired( false );
            kryo.setReferences( true );
            kryo.register( RemoteInvocation.class );
            kryo.register( RemoteInvocationException.class );
            kryo.setInstantiatorStrategy( new DefaultInstantiatorStrategy( new StdInstantiatorStrategy() ) );
            return kryo;
        }
    };

    Pool<Output> outputPool = new Pool<>( true, false, 16 ) {
        protected Output create() {
            return new Output( 1024, -1 );
        }
    };

    Pool<Input> inputPool = new Pool<>( true, false, 16 ) {
        protected Input create() {
            return new Input( 1024 );
        }
    };
}
