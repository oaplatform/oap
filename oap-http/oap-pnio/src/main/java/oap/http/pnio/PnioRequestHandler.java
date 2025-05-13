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

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioRequestHandler<State> {
    public String description() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return description();
    }

    public Type getType() {
        return Type.IO;
    }

    public abstract void handle( PnioExchange<State> pnioExchange, State state ) throws InterruptedException, IOException;

    public enum Type {
        IO, COMPUTE
    }
}
