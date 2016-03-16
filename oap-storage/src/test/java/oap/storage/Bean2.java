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

package oap.storage;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.storage.migration.FileStorageMigration;
import oap.storage.migration.JsonMetadata;

/**
 * Created by Igor Petrenko on 05.10.2015.
 */
@ToString
@EqualsAndHashCode
class Bean2 {
    public String id2;
    public BeanIn in;

    public Bean2( String id, String s ) {
        this.id2 = id;
        this.in = new BeanIn();
        this.in.s = s;
    }

    public Bean2( String id ) {
        this( id, "aaa" );
    }

    public Bean2() {
    }

    @ToString
    @EqualsAndHashCode
    public static class BeanIn {
        public String s = "aaa";
    }

    public static class Bean2Migration implements FileStorageMigration {

        @Override
        public long fromVersion() {
            return 1;
        }

        @Override
        public JsonMetadata run( JsonMetadata oldV ) {
            return oldV
                .mapString( "object:type", ( str ) -> "oap.storage.Bean2" )
                .object()
                .rename( "id", "id2" )
                .rename( "s", "in.s" )
                .topParent();
        }
    }
}
