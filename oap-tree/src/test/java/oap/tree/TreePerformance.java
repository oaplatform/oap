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

package oap.tree;

import lombok.SneakyThrows;
import lombok.val;
import oap.testng.AbstractPerformance;
import oap.util.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static oap.tree.Dimension.ARRAY_ENUM;
import static oap.tree.Dimension.ARRAY_LONG;
import static oap.tree.Dimension.ARRAY_STRING;
import static oap.tree.Dimension.BOOLEAN;
import static oap.tree.Dimension.ENUM;
import static oap.tree.Dimension.LONG;
import static oap.tree.Dimension.OperationType.NOT_CONTAINS;
import static oap.tree.Dimension.STRING;

/**
 * Created by igor.petrenko on 26.12.2016.
 */
public class TreePerformance extends AbstractPerformance {
    public static final Pattern ARRAY_STRING_PATTERN = Pattern.compile( "^arrayString(\\d+)-\\d+$" );
    public static final Pattern ARRAY_LONG_PATTERN = Pattern.compile( "^arrayLong(\\d+)-\\d+$" );
    public static final Pattern ARRAY_ENUM_PATTERN = Pattern.compile( "^arrayEnum(\\d+)-([^\\-]+)-\\d+$" );
    public static final Pattern ENUM_PATTERN = Pattern.compile( "^enum-([^\\-]+)-\\d+$" );
    public static final int SAMPLES = 100000;
    private static final AtomicInteger dimensionId = new AtomicInteger( 0 );

    private static <T> Set<T> s( T... data ) {
        return new HashSet<>( asList( data ) );
    }

    @Test
    public void tree() {
        run( 0.25, Integer.MIN_VALUE, 1000 );
        run( 0.5, Integer.MIN_VALUE, 1000 );
        run( 1, Integer.MIN_VALUE, 1000 );
        run( 0.75, Integer.MIN_VALUE, 1000 );
    }

    public void run( double fillFactor, int arrayToTree, int dataCount ) {
        val bids = new ArrayList<ArrayList<Object>>();

        val dimensions = new ArrayList<Dimension>();

        addStringDimension( dimensions, 20, Dimension.OperationType.CONTAINS );
        addLongDimension( dimensions, 5 );
        addStringDimension( dimensions, 30, NOT_CONTAINS );
        addBooleanDimension( dimensions, 10, Dimension.OperationType.CONTAINS );
        addBooleanDimension( dimensions, 10, NOT_CONTAINS );
        addEnumDimension( dimensions, TestEnum1.class, TestEnum1.UNKNOWN1 );
        addEnumDimension( dimensions, TestEnum2.class, TestEnum2.Test21 );
        addArrayEnumDimension( dimensions, TestEnum3.class, TestEnum3.Test31, 2 );
        addArrayStringDimension( dimensions, 5, 300 );
        addArrayStringDimension( dimensions, 5, 3 );
        addArrayStringDimension( dimensions, 5, 4 );
        addArrayStringDimension( dimensions, 5, 5 );
        addArrayLongDimension( dimensions, 5, 4 );
        addArrayLongDimension( dimensions, 5, 10 );


        val data = new ArrayList<Tree.ValueData<String>>();
        fill( dimensions, data, dataCount );

        generateRequests( 100, dimensions, bids, data );

        final Tree<String> tree = Tree
            .<String>tree( dimensions )
            .withHashFillFactor( fillFactor )
            .withArrayToTree( arrayToTree )
            .load( data );


        benchmark( "tree-" + fillFactor + "-" + dataCount, SAMPLES, ( i ) -> {
            Assertions.assertThat( tree.find( bids.get( i % bids.size() ) ) ).isNotEmpty();
        } ).run();

        System.out.println( tree.getMaxDepth() );
    }

    private void generateRequests( int requests,
                                   ArrayList<Dimension> dimensions,
                                   ArrayList<ArrayList<Object>> bids,
                                   ArrayList<Tree.ValueData<String>> data ) {
        final int size = data.size();
        val vals = data.get( RandomUtils.nextInt( 0, size ) );

        for( int i = 0; i < requests; i++ ) {
            val request = new ArrayList<Object>();

            for( int d = 0; d < dimensions.size(); d++ ) {
                val v = vals.data.get( d );
                val dimension = dimensions.get( d );

                if( v instanceof List<?> ) {
                    if( dimension.operationType == NOT_CONTAINS ) {
                        request.add( "!!!!!!!!-fix" );
                    } else {
                        val list = ( List<?> ) v;
                        request.add( Lists.random( list ).get() );
                    }
                } else {
                    if( dimension.operationType == NOT_CONTAINS ) {
                        if( dimension.name.startsWith( "boolean" ) ) {
                            request.add( !( boolean ) ( Boolean ) v );
                        } else {
                            request.add( "!!!!!!!!-fix" );
                        }
                    } else {
                        request.add( v );
                    }
                }
            }

            bids.add( request );
        }

    }

    private void addBooleanDimension( ArrayList<Dimension> dimensions, int count, Dimension.OperationType operationType ) {
        for( int i = 0; i < count; i++ )
            dimensions.add( BOOLEAN( "boolean" + dimensionId.incrementAndGet(), operationType, null ) );

    }

    @SneakyThrows
    private void fill( ArrayList<Dimension> dimensions, ArrayList<Tree.ValueData<String>> data, int count ) {
        val map = new HashMap<String, ArrayList<String>>();
        val mapSize = new HashMap<String, Integer>();
        val enums = new HashMap<String, ArrayList<Enum<?>>>();

        for( int i = 0; i < count; i++ ) {
            val selection = new ArrayList<Object>();
            for( Dimension dimension : dimensions ) {
                if( dimension.name.startsWith( "string" ) ) {
                    val list = map.computeIfAbsent( dimension.name, ( dn ) -> new ArrayList<>() );
                    if( list.isEmpty() ) {
                        for( int r = 0; r < 100; r++ ) {
                            list.add( RandomStringUtils.randomAlphabetic( 10 ) + i );
                        }
                    }
                    selection.add( Lists.random( list ).get() );
                } else if( dimension.name.startsWith( "long" ) ) {
                    selection.add( RandomUtils.nextLong( 0, 1000000 ) );
                } else if( dimension.name.startsWith( "boolean" ) ) {
                    selection.add( RandomUtils.nextBoolean() );
                } else if( dimension.name.startsWith( "arrayString" ) ) {
                    val size = mapSize.computeIfAbsent( dimension.name, ss -> {
                        final Matcher matcher = ARRAY_STRING_PATTERN.matcher( dimension.name );
                        assert matcher.find();
                        return Integer.parseInt( matcher.group( 1 ) );
                    } );
                    val list = map.computeIfAbsent( dimension.name, ( dn ) -> new ArrayList<>() );
                    if( list.isEmpty() ) {
                        for( int r = 0; r < 100; r++ ) {
                            list.add( RandomStringUtils.randomAlphabetic( 10 ) + i );
                        }
                    }

                    val array = new ArrayList<String>();
                    for( int s = 0; s < size; s++ ) {
                        array.add( Lists.random( list ).get() );
                    }
                    selection.add( new Tree.Array( array, true ) );
                } else if( dimension.name.startsWith( "arrayEnum" ) ) {
                    val size = mapSize.computeIfAbsent( dimension.name, ss1 -> {
                        final Matcher matcher = ARRAY_ENUM_PATTERN.matcher( dimension.name );
                        assert matcher.find();
                        return Integer.parseInt( matcher.group( 1 ) );
                    } );

                    val list = enums.computeIfAbsent( dimension.name, ( dn ) -> new ArrayList<>() );
                    if( list.isEmpty() ) {
                        final Matcher matcher = ARRAY_ENUM_PATTERN.matcher( dimension.name );
                        assert matcher.find();
                        final Class<Enum<?>> clazz = ( Class<Enum<?>> ) Class.forName( matcher.group( 2 ) );
                        final Enum<?>[] enumConstants = clazz.getEnumConstants();
                        list.addAll( asList( enumConstants ) );
                    }


                    val array = new ArrayList<Enum<?>>();
                    for( int s = 0; s < size; s++ ) {
                        array.add( Lists.random( list ).get() );
                    }
                    selection.add( new Tree.Array( array, true ) );
                } else if( dimension.name.startsWith( "arrayLong" ) ) {
                    val array = new ArrayList<Long>();

                    val size = mapSize.computeIfAbsent( dimension.name, ss1 -> {
                        final Matcher matcher = ARRAY_LONG_PATTERN.matcher( dimension.name );
                        matcher.find();
                        return Integer.parseInt( matcher.group( 1 ) );
                    } );

                    for( int s = 0; s < size; s++ ) {
                        array.add( RandomUtils.nextLong( 0, size ) );
                    }
                    selection.add( new Tree.Array( array, true ) );
                } else if( dimension.name.startsWith( "enum" ) ) {
                    val list = enums.computeIfAbsent( dimension.name, ( dn ) -> new ArrayList<>() );
                    if( list.isEmpty() ) {
                        final Matcher matcher = ENUM_PATTERN.matcher( dimension.name );
                        assert matcher.find();
                        final Class<Enum<?>> clazz = ( Class<Enum<?>> ) Class.forName( matcher.group( 1 ) );
                        final Enum<?>[] enumConstants = clazz.getEnumConstants();
                        list.addAll( asList( enumConstants ) );
                    }

                    selection.add( Lists.random( list ).get() );
                } else {
                    throw new IllegalStateException( "Unknown dimension type " + dimension.name );
                }
            }
            data.add( new Tree.ValueData<>( selection, "data" + i ) );
        }
    }

    private void addArrayLongDimension( ArrayList<Dimension> dimensions, int count, int arraySize ) {
        for( int i = 0; i < count; i++ )
            dimensions.add( ARRAY_LONG( "arrayLong" + arraySize + "-" + dimensionId.incrementAndGet(), null ) );
    }

    private void addArrayStringDimension( ArrayList<Dimension> dimensions, int count, int arraySize ) {
        for( int i = 0; i < count; i++ )
            dimensions.add( ARRAY_STRING( "arrayString" + arraySize + "-" + dimensionId.incrementAndGet() ) );
    }

    private <T extends Enum<T>> void addArrayEnumDimension( ArrayList<Dimension> dimensions, Class<T> enumClass, T nullValue, int arraySize ) {
        dimensions.add( ARRAY_ENUM( "arrayEnum" + arraySize + "-" + enumClass.getTypeName() + "-" + dimensionId.incrementAndGet(), enumClass, nullValue ) );
    }

    private <T extends Enum<T>> void addEnumDimension( ArrayList<Dimension> dimensions, Class<T> enumClass, T defaultValue ) {
        dimensions.add( ENUM( "enum-" + enumClass.getTypeName() + "-" + dimensionId.incrementAndGet(), enumClass, Dimension.OperationType.CONTAINS, defaultValue ) );
    }

    private void addLongDimension( ArrayList<Dimension> dimensions, int count ) {
        for( int i = 0; i < count; i++ )
            dimensions.add( LONG( "long" + dimensionId.incrementAndGet(), Dimension.OperationType.CONTAINS, null ) );
    }

    private void addStringDimension( ArrayList<Dimension> dimensions, int count, Dimension.OperationType operationType ) {
        for( int i = 0; i < count; i++ )
            dimensions.add( STRING( "string" + dimensionId.incrementAndGet(), operationType ) );
    }

    public enum TestEnum1 {
        Test11, Test12, Test13, Test14, UNKNOWN1
    }

    public enum TestEnum2 {
        Test21, Test22, Test23
    }

    public enum TestEnum3 {
        Test31, Test32, Test33, Test34, Test35, Test36, Test37, Test38
    }
}