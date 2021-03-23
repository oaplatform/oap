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

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class FixtureWithScope<T extends FixtureWithScope<T>> implements Fixture {
    protected Scope scope = Scope.METHOD;

    @SuppressWarnings( "unchecked" )
    public T withScope( Scope scope ) {
        this.scope = scope;

        return ( T ) this;
    }

    public final Scope getScope() {
        return scope;
    }

    @Override
    public void beforeSuite() {
        if( scope == Scope.SUITE ) before();
    }

    @Override
    public void afterSuite() {
        if( scope == Scope.SUITE ) after();
    }

    @Override
    public void beforeClass() {
        if( scope == Scope.CLASS ) before();
    }

    @Override
    public void afterClass() {
        if( scope == Scope.CLASS ) after();
    }

    @Override
    public void beforeMethod() {
        if( scope == Scope.METHOD ) before();
    }

    @Override
    public void afterMethod() {
        if( scope == Scope.METHOD ) after();
    }

    protected abstract void before();

    protected abstract void after();
}
