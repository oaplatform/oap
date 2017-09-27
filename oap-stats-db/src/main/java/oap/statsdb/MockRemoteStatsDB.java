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

package oap.statsdb;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by igor.petrenko on 08.09.2017.
 */
public class MockRemoteStatsDB implements RemoteStatsDB {
    public final ArrayList<Sync> syncs = new ArrayList<>();
    private final KeySchema schema;
    private Function<Sync, RuntimeException> exceptionFunc;

    public MockRemoteStatsDB( KeySchema schema ) {
        this.schema = schema;
    }

    @Override
    public boolean update( Sync data, String host ) {
        if( exceptionFunc != null ) throw exceptionFunc.apply( data );

        syncs.add( data );

        return true;
    }

    @Override
    public KeySchema getSchema() {
        return schema;
    }

    public void syncWithException( Function<Sync, RuntimeException> exceptionFunc ) {
        this.exceptionFunc = exceptionFunc;
    }

    public void syncWithoutException() {
        this.exceptionFunc = null;
    }

    public void reset() {
        syncs.clear();
        exceptionFunc = null;
    }
}
