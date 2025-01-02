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

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class AsyncPnioRequestHandler<State> extends AbstractPnioRequestHandler<State> {
    public abstract void handle( PnioExchange<State> pnioExchange, State state,
                                 Runnable success, Consumer<Throwable> exception ) throws InterruptedException, IOException;
}
