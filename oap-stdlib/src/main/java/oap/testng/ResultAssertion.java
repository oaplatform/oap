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

package oap.testng;

import oap.util.Result;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.error.BasicErrorMessageFactory;

import static oap.testng.ResultAssertion.ResultShouldBeFailure.shouldBeFailure;
import static oap.testng.ResultAssertion.ResultShouldBeSuccess.shouldBeSuccess;

public class ResultAssertion<S, F> extends AbstractAssert<ResultAssertion<S, F>, Result<S, F>> {
    public ResultAssertion( Result<S, F> actual ) {
        super( actual, ResultAssertion.class );
    }

    public ResultAssertion<S, F> isSuccess() {
        if( !actual.isSuccess() ) throwAssertionError( shouldBeSuccess( actual ) );
        return myself;
    }

    public ResultAssertion<S, F> isFailure() {
        if( actual.isSuccess() ) throwAssertionError( shouldBeFailure( actual ) );
        return myself;
    }

    public static class ResultShouldBeSuccess extends BasicErrorMessageFactory {
        private ResultShouldBeSuccess( Class<?> resultClass, Object resultFailureValue ) {
            super( "%nExpecting success " + resultClass.getSimpleName() + " but was containing failure: <%s>.", resultFailureValue );
        }

        public static <S, F> ResultShouldBeSuccess shouldBeSuccess( Result<S, F> result ) {
            return new ResultShouldBeSuccess( result.getClass(), result.failureValue );
        }
    }

    public static class ResultShouldBeFailure extends BasicErrorMessageFactory {
        private ResultShouldBeFailure( Class<?> resultClass, Object resultSuccessValue ) {
            super( "%nExpecting failure " + resultClass.getSimpleName() + " but was containing success: <%s>.", resultSuccessValue );
        }

        public static <S, F> ResultShouldBeFailure shouldBeFailure( Result<S, F> result ) {
            return new ResultShouldBeFailure( result.getClass(), result.failureValue );
        }
    }
}
