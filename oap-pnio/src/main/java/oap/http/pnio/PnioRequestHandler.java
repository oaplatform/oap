/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import java.io.IOException;
import java.util.function.BiConsumer;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioRequestHandler<State> {
    public String description() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return description();
    }

    public abstract Type getType();

    public abstract void handle( PnioExchange<State> pnioExchange, State state ) throws InterruptedException, IOException;

    enum Type {
        IO, COMPUTE
    }

    public static <State> PnioRequestHandler create( BiConsumer<PnioExchange<State>, State> function, Type type ) {
        if ( type == Type.COMPUTE ) return new PnioComputeRequestHandler<>( function );
        return new PnioInputOutputRequestHandler<>( function );
    }
}
