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
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import com.jasonclawson.jackson.dataformat.hocon.HoconTreeTraversingParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import oap.util.Stream;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.Map;

public class HoconFactoryWithFallback extends HoconFactory {
    private final Config additinal;
    private final boolean withSystemProperties;
    private final Logger log;

    public HoconFactoryWithFallback( boolean withSystemProperties, Logger log, Map<String, Object> config ) {
        this( withSystemProperties, log, ConfigFactory.parseMap( config ) );
    }

    public HoconFactoryWithFallback( boolean withSystemProperties, Logger log, String... config ) {
        this( withSystemProperties, log, init( config ) );
    }

    public HoconFactoryWithFallback( boolean withSystemProperties, Logger log, Config additinal ) {
        this.withSystemProperties = withSystemProperties;
        this.log = log;
        this.additinal = additinal;

//        if( log.isTraceEnabled() ) System.setProperty( "config.trace", "loads" );
    }

    private static Config init( String[] configs ) {
        return Stream.of( configs )
            .foldLeft( ConfigFactory.empty(),
                ( config, value ) -> config.withFallback( ConfigFactory.parseString( value ) ) );
    }

    @Override
    protected HoconTreeTraversingParser _createParser( Reader r, IOContext ctxt ) {
        ConfigParseOptions options = ConfigParseOptions.defaults();
        Config config = ConfigFactory.parseReader( r, options );

        Config unresolvedConfig = additinal.withFallback( config );

        if( withSystemProperties )
            unresolvedConfig = unresolvedConfig.withFallback( ConfigFactory.systemProperties() );

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
