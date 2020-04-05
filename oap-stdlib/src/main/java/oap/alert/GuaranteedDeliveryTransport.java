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

package oap.alert;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GuaranteedDeliveryTransport {

    private final Retryer<Boolean> retryer;

    public GuaranteedDeliveryTransport( long maxWaitTimeSeconds ) {
        this( maxWaitTimeSeconds, Integer.MAX_VALUE );
    }

    public GuaranteedDeliveryTransport( long maxWaitTimeSeconds, int maxAttempts ) {
        retryer = RetryerBuilder.<Boolean>newBuilder()
            .withRetryListener( new RetryListener() {
                @Override
                public <V> void onRetry( Attempt<V> attempt ) {
                    String errorMessage = attempt.hasException() ? attempt.getExceptionCause().toString() : "no errors";
                    V result = attempt.hasResult() ? attempt.getResult() : null;
                    log.warn( "attempt: {},  result: {}, error: {}", attempt.getAttemptNumber(), result, errorMessage );
                }
            } )
            .retryIfException( e -> !( e instanceof InterruptedException ) )
            .withWaitStrategy( WaitStrategies.fibonacciWait( maxWaitTimeSeconds, TimeUnit.SECONDS ) )
            .withStopStrategy( StopStrategies.stopAfterAttempt( maxAttempts ) )
            .build();
    }


    public <Message> void send( Message m, MessageTransport<Message> transport ) throws InterruptedException {
        try {
            retryer.call( () -> {
                transport.send( m );
                return true;
            } );
        } catch( ExecutionException e ) {
            if( e.getCause() instanceof InterruptedException ) throw ( InterruptedException ) e.getCause();
            log.error( "unexpected execution exception", e );
        } catch( RetryException e ) {
            log.error( "cannot exec retry", e );
        }

    }
}
