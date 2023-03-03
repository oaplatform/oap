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
package oap.concurrent;

import java.util.HashSet;
import java.util.Set;

@Deprecated
// using it is dangerous: inner static collection is meant to cause memory leak as it never clears
public class Once {
    private static final Set<Object> done = new HashSet<>();

    public static void executeOnce( Runnable action ) {
        boolean needRun = false;
        synchronized( Once.class ) {
            needRun = !done.contains( action.getClass() );
        }
        if( needRun ) {
            action.run();
            synchronized( Once.class ) {
                done.add( action.getClass() );
            }
        }
    }

    public static Runnable once( Runnable action ) {
        return new Runnable() {
            volatile boolean done = false;

            @Override
            public void run() {
                if( !done ) {
                    done = true;
                    action.run();
                }
            }
        };
    }
}
