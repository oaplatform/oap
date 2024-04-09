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

package oap.logstream.data.map;

import oap.dictionary.DictionaryRoot;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.Logger;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class AbstractMapLogger extends Logger {
    private final MapLogRenderer renderer;
    private final String name;

    public AbstractMapLogger( AbstractLoggerBackend backend, DictionaryRoot datamodel, String id, String tag, String name ) {
        super( backend );
        this.name = name;
        this.renderer = new MapLogModel( datamodel ).renderer( id, tag );
    }

    public void log( @Nonnull Map<String, Object> data ) {
        this.log( prefix( data ), substitutions( data ), name, renderer.headers(), renderer.types(), renderer.render( data ) );
    }

    @Nonnull
    public abstract String prefix( @Nonnull Map<String, Object> data );

    @Nonnull
    public abstract Map<String, String> substitutions( @Nonnull Map<String, Object> data );

}
