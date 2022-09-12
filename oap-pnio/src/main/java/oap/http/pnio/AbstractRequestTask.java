/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

public abstract class AbstractRequestTask<State> {
    public String description() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return description();
    }

    public abstract boolean isCpu();

    public abstract void accept( RequestTaskState<State> stateRequestTaskState, State state ) throws InterruptedException;
}
