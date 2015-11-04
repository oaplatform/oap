/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.etl;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import oap.io.IoStreams;
import oap.tsv.Model;
import oap.tsv.Tsv;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static oap.io.Files.version;

public class Table {
    private Stream<List<Object>> lines;
    private List<Runnable> closeHandlers = new ArrayList<>();

    private Table( Stream<List<Object>> lines ) {
        this.lines = lines;
    }

    public static Optional<Table> fromResource( Class<?> contextClass, String name, Model.Version modelVersion ) {
        return Tsv.fromResource( contextClass, name, modelVersion ).map( Table::new );
    }

    public static Table fromFile( Path path, Model model ) {
        return new Table( Tsv.fromPath( path, model.withVersion(version(path) ) ) );
    }

    public static Table fromFiles( List<Path> paths, IoStreams.Encoding encoding, Model model ) {
        return new Table( Tsv.fromPaths( paths, encoding, model ) );
    }

    @SuppressWarnings( "unchecked" )
    public Table sort( int[] fields ) {
        this.lines = lines.sorted( ( l1, l2 ) -> {
            for( int field : fields ) {
                int result = ((Comparable) l1.get( field )).compareTo( l2.get( field ) );
                if( result != 0 ) return result;
            }
            return 0;
        } );
        return this;
    }

    public Table export( Export export ) {
        closeHandlers.add( export::close );
        return transform( export::line );
    }

    public Table progress( long step, LongConsumer report ) {
        AtomicLong total = new AtomicLong( 0 );
        closeHandlers.add( () -> report.accept( total.get() ) );
        return transform( l -> {
            if( total.incrementAndGet() % step == 0 ) report.accept( total.get() );
        } );
    }

    public Table transform( Consumer<List<Object>> consumer ) {
        this.lines = this.lines.map( l -> {
            consumer.accept( l );
            return l;
        } );
        return this;
    }

    public Table groupBy( int[] fields, Accumulator... accumulators ) {
        PeekingIterator<List<Object>> iterator = Iterators.peekingIterator( sort( fields ).lines.iterator() );
        class DistinctIterator implements Iterator<List<Object>> {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<Object> next() {
                for( Accumulator formula : accumulators ) formula.reset();
                while( iterator.hasNext() ) {
                    List<Object> current = iterator.next();
                    for( Accumulator accumulator : accumulators ) accumulator.accumulate( current );
                    if( !iterator.hasNext() || !distinctiveEquals( current, iterator.peek() ) )
                        return result( current, accumulators );
                }
                throw new NoSuchElementException();
            }

            private List<Object> result( List<Object> l, Accumulator[] accumulators ) {
                ArrayList<Object> result = new ArrayList<>();
                for( int field : fields ) result.add( l.get( field ) );
                for( Accumulator accumulator : accumulators ) result.add( accumulator.result() );
                return result;
            }

            private boolean distinctiveEquals( List<Object> a, List<Object> b ) {
                for( int field : fields ) if( !Objects.equals( a.get( field ), b.get( field ) ) ) return false;
                return true;
            }
        }
        this.lines = Stream.of( new DistinctIterator() );
        return this;
    }

    public Table join( int keyPos, Join... joins ) {
        return transform( line -> {
            for( Join join : joins ) line.addAll( join.on( (String) line.get( keyPos ) ) );
        } );
    }

    public void compute() {
        lines.drain();
        for( Runnable closeHandler : closeHandlers ) closeHandler.run();
    }
}
