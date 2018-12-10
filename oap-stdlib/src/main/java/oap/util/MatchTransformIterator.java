package oap.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

public class MatchTransformIterator<A, B, R> implements Iterator<R> {
    private final PeekingIterator<A> master;
    private final PeekingIterator<B> lookup;
    private final Predicate<A> validator;
    private final Comparator<A, B> comparator;
    private final BiFunction<A, B, R> transform;

    public MatchTransformIterator( Iterator<A> master, Iterator<B> lookup, Predicate<A> validator, Comparator<A, B> comparator, BiFunction<A, B, R> transform ) {
        this.master = Iterators.peekingIterator( master );
        this.lookup = Iterators.peekingIterator( lookup );
        this.validator = validator;
        this.comparator = comparator;
        this.transform = transform;
    }

    @Override
    public boolean hasNext() {
        int comparation;
        while( master.hasNext() && lookup.hasNext()
            && ( comparation = comparator.compare( master.peek(), lookup.peek() ) ) != 0 ) {
            if( !validator.test( master.peek() ) ) master.next();
            else if( comparation < 0 ) master.next();
            else lookup.next();
        }
        return master.hasNext() && lookup.hasNext();
    }

    @Override
    public R next() {
        return transform.apply( master.next(), lookup.next() );
    }

    public static <A, B, R> Stream<R> stream( Stream<A> master, Stream<B> lookup, Predicate<A> validator, Comparator<A, B> comparator, BiFunction<A, B, R> transform ) {
        return StreamSupport.stream( spliteratorUnknownSize(
            iterator( master.iterator(), lookup.iterator(), validator, comparator, transform ),
            ORDERED ), false );
    }

    public static <A, B, R> MatchTransformIterator<A, B, R> iterator( Iterator<A> master, Iterator<B> lookup, Predicate<A> validator, Comparator<A, B> comparator, BiFunction<A, B, R> transform ) {
        return new MatchTransformIterator<>( master, lookup, validator, comparator, transform );
    }


    @FunctionalInterface
    public interface Comparator<A, B> {
        int compare( A a, B b );
    }

}
