package oap.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class Collectors {
    public static final Set<Collector.Characteristics> CH_ID
            = Collections.unmodifiableSet( EnumSet.of( Collector.Characteristics.IDENTITY_FINISH ) );

    public static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        final private Supplier<A> supplier;
        final private BiConsumer<A, T> accumulator;
        final private BinaryOperator<A> combiner;
        private Function<A, R> finisher;
        private Set<Characteristics> characteristics;

        public CollectorImpl( Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
                              Function<A, R> finisher, Set<Characteristics> characteristics ) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        public CollectorImpl( Supplier<A> supplier, BiConsumer<A, T> accumulator,
                              BinaryOperator<A> combiner,
                              Set<Characteristics> characteristics ) {
            this(supplier, accumulator, combiner, Functions.castingIdentity(), characteristics);
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
}
