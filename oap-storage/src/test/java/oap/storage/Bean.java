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
public class Bean {
    public static final Identifier<Bean> identifier = IdentifierBuilder.<Bean>identify( p -> p.s ).build();

    public String id;
    public String s = "aaa";

    public Bean( String id, String s ) {
        this.id = id;
        this.s = s;
    }

    public Bean( String id ) {
        this( id, "aaa" );
    }

    public Bean() {
    }

    public static class BeanMigration implements FileStorageMigration {

        @Override
        public long fromVersion() {
            return 0;
        }

        @Override
        public JsonMetadata run( JsonMetadata old ) {
            return old
                .object()
                .mapString( "id", s -> s + "1" )
                .topParent();
        }
    }
}
