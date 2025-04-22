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

package oap.json;

import com.fasterxml.jackson.core.io.IOContext;
import com.jasonclawson.jackson.dataformat.hocon.HoconTreeTraversingParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import lombok.SneakyThrows;
import org.slf4j.Logger;

import java.io.Reader;

public class HoconFactoryWithSystemProperties extends OapHoconFactory {
    private final Logger log;

    public HoconFactoryWithSystemProperties( Logger log ) {
        this.log = log;
//        if( log.isTraceEnabled() ) System.setProperty( "config.trace", "loads" );
    }

    @SneakyThrows
    @Override
    protected HoconTreeTraversingParser _createParser( Reader r, IOContext ctxt ) {
        ConfigParseOptions options = ConfigParseOptions.defaults();

        Object rawContent = ctxt.contentReference().getRawContent();
        log.trace( "rawContent {} rawContentClazz {}", rawContent, rawContent.getClass() );

        options = fixClassLoader( log, rawContent, options );

        Config config = ConfigFactory.parseReader( r, options );

        Config unresolvedConfig = config.withFallback( ConfigFactory.systemProperties() );
//        log.trace( unresolvedConfig.root().render() );
        try {
            Config resolvedConfig = unresolvedConfig.resolve();
            return new HoconTreeTraversingParser( resolvedConfig.root(), _objectCodec );
        } catch( ConfigException e ) {
            log.error( unresolvedConfig.root().render() );
            throw e;
        }
    }

}
