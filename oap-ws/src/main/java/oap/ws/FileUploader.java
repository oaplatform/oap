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

package oap.ws;

import lombok.ToString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Created by Admin on 08.02.2017.
 */
public abstract class FileUploader {
    private final ArrayList<FileUploaderListener> listeners = new ArrayList<>();

    public synchronized void addListener( FileUploaderListener listener ) {
        listeners.add( listener );
    }

    public synchronized void removeListener( FileUploaderListener listener ) {
        listeners.remove( listener );
    }

    protected synchronized void fireUploaded( Item file ) {
        listeners.forEach( l -> l.uploaded( file ) );
    }

    public interface FileUploaderListener {
        void uploaded( Item file );
    }

    @ToString( exclude = { "inputStreamFunc" } )
    public static class Item {
        public final String prefix;
        public final String id;
        public final String name;
        public final String contentType;
        public final Supplier<InputStream> inputStreamFunc;

        public Item( String prefix, String id, String name, String contentType, Supplier<InputStream> inputStreamFunc ) {
            this.prefix = prefix;
            this.id = id;
            this.name = name;
            this.contentType = contentType;
            this.inputStreamFunc = inputStreamFunc;
        }
    }
}
