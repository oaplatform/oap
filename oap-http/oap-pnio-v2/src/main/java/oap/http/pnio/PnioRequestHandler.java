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
import java.util.function.Consumer;

public class PnioRequestHandler<State> {
    public final Type type;

    public PnioRequestHandler( Type type ) {
        this.type = type;
    }

    public String description() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return description();
    }

    public void handle( PnioExchange<State> pnioExchange, State state ) throws InterruptedException, IOException {
    }

    public void handle( PnioExchange<State> pnioExchange, State state,
                        Runnable success, Consumer<Throwable> exception ) throws InterruptedException, IOException {
    }

    public enum Type {
        COMPUTE, BLOCKING, ASYNC
    }
}
