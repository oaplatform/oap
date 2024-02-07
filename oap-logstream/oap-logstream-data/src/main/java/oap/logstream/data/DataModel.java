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

package oap.logstream.data;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryParser;
import oap.dictionary.DictionaryRoot;
import oap.io.Resources;
import oap.io.content.Resource;

import javax.annotation.Nonnull;

import java.net.URL;
import java.nio.file.Path;

import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;

@Slf4j
public class DataModel {
    public final Dictionary model;

    public DataModel( @Nonnull DictionaryRoot model ) {
        this.model = model;
    }

    public DataModel( @Nonnull Resource resource ) {
        log.debug( "loading by resource url {}", resource.url );
        this.model = DictionaryParser.parse( resource.url, INCREMENTAL_ID_STRATEGY );
    }

    @SneakyThrows
    public DataModel( @Nonnull Path location ) {
        log.debug( "loading by location {}", location );
        this.model = DictionaryParser.parse( location.toUri().toURL(), INCREMENTAL_ID_STRATEGY );
    }

    public DataModel( @Nonnull URL url ) {
        log.debug( "loading by url {}", url );
        this.model = DictionaryParser.parse( url, INCREMENTAL_ID_STRATEGY );
    }

    public DataModel( @Nonnull String resourceLocation ) {
        this( Resources.url( DataModel.class, resourceLocation )
            .orElseThrow( () -> new IllegalArgumentException( resourceLocation ) ) );
    }
}
