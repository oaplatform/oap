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

package oap.logstream.sharding;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameShardMapper implements ShardMapper {

    private final Pattern pattern;
    private final LoadingCache<String, Integer> filenameToShardCache;

    public FilenameShardMapper( String regexp ) {
        pattern = Pattern.compile( regexp );
        filenameToShardCache = CacheBuilder
            .newBuilder()
            .maximumSize( 1000000 )
            .expireAfterWrite( 10, TimeUnit.MINUTES )
            .build( new CacheLoader<String, Integer>() {
                public Integer load( String key ) {
                    return parseShardNumber( key );
                }
            } );
    }

    private int parseShardNumber( String filename ) {
        Matcher m = pattern.matcher( filename );

        if( m.find() ) {
            return Integer.parseInt( m.group( 1 ) );
        } else {
            throw new IllegalArgumentException( filename + " doesn't contain shard number" );
        }
    }

    @Override
    public int getShardNumber( String hostName, String fileName ) {
        try {
            return filenameToShardCache.get( fileName );
        } catch( UncheckedExecutionException | ExecutionException e ) {
            throw new IllegalArgumentException( e.getCause() );
        }
    }
}
