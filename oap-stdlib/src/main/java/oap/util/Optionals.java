package oap.util;

import java.util.Optional;
import java.util.function.Consumer;

public class Optionals {
    public static <T> java.util.stream.Stream<T> toStream( Optional<T> opt ) {
        return opt.map( Stream::of ).orElse( Stream.empty() );
    }

    public static <T> Fork<T> fork( Optional<T> opt ) {
        return new Fork<>( opt );
    }

    public static class Fork<T> {
        private Optional<T> opt;

        public Fork( Optional<T> opt ) {
            this.opt = opt;
        }

        public Fork<T> ifPresent( Consumer<? super T> consumer ) {
            opt.ifPresent( consumer );
            return this;
        }

        public Fork<T> ifAbsent( Runnable run ) {
            if( !opt.isPresent() ) run.run();
            return this;
        }
    }
}
