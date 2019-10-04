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

package oap.metrics;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import oap.util.Throwables;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by igor.petrenko on 08.04.2019.
 * <p>
 * Env:
 * - APPLICATION: (app name)
 * - HashMapMetrics: (true/false), default: false
 */
public class HashMapMetrics {
    private static final boolean enabled;
    private static final Field tableField;
    private static final Field nextField;

    static {
        enabled = "true".equalsIgnoreCase( System.getenv( "HashMapMetrics" ) );

        try {
            tableField = HashMap.class.getDeclaredField( "table" );
            tableField.setAccessible( true );

            Class<?> hashMapEntryClass = null;
            for( Class<?> c : HashMap.class.getDeclaredClasses() )
                if( "java.util.HashMap.Node".equals( c.getCanonicalName() ) )
                    hashMapEntryClass = c;

            Preconditions.checkNotNull( hashMapEntryClass, "java.util.HashMap.Node not found" );

            nextField = hashMapEntryClass.getDeclaredField( "next" );
            nextField.setAccessible( true );
        } catch( NoSuchFieldException e ) {
            throw Throwables.propagate( e );
        }
    }

    @SneakyThrows
    public static HashmapStats dumpBuckets( HashMap<?, ?> map ) {
        val table = ( Map.Entry<?, ?>[] ) tableField.get( map );

        int empty = 0;
        int max = 1;
        int collisions = 0;

        for( Map.Entry<?, ?> value : table ) {
            Map.Entry<?, ?> entry = value;

            if( entry == null ) empty++;

            int size = 0;

            while( entry != null ) {
                entry = ( Map.Entry<?, ?> ) nextField.get( entry );
                size++;
            }

            max = Math.max( max, size );
            if( size > 1 ) collisions++;
        }

        return new HashmapStats( table.length, empty, max, collisions );
    }

    public static void meter( String hashMapName, HashMap<?, ?> hashmap ) {
        if( !enabled ) return;

        Metrics.measureGauge( metricName( hashMapName, "hashmap.size" ), hashmap::size );
        Metrics.measureGauge( metricName( hashMapName, "hashmap.entries" ), () -> dumpBuckets( hashmap ).entries );
        Metrics.measureGauge( metricName( hashMapName, "hashmap.collisions_max" ), () -> dumpBuckets( hashmap ).max );
        Metrics.measureGauge( metricName( hashMapName, "hashmap.collisions_count" ), () -> dumpBuckets( hashmap ).collisions );
    }

    public static void unregister( String hashMapName ) {
        Metrics.unregister( metricName( hashMapName, "hashmap.size" ) );
        Metrics.unregister( metricName( hashMapName, "hashmap.entries" ) );
        Metrics.unregister( metricName( hashMapName, "hashmap.collisions_max" ) );
        Metrics.unregister( metricName( hashMapName, "hashmap.collisions_count" ) );
    }

    private static Name metricName( String hashMapName, String name ) {
        return Metrics.name( name ).tag( "name", hashMapName );
    }

    @ToString
    public static class HashmapStats {
        public final int entries;
        public final int empty;
        public final int max;
        public final int collisions;

        public HashmapStats( int entries, int empty, int max, int collisions ) {
            this.entries = entries;
            this.empty = empty;
            this.max = max;
            this.collisions = collisions;
        }
    }
}
