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

import oap.util.Lists;

import java.util.ArrayList;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class FixtureWithScope<T extends FixtureWithScope<T>> implements Fixture {
    protected final ArrayList<FixtureWithScope<?>> fixtures = new ArrayList<>();

    protected Scope scope = Scope.METHOD;

    public <F extends FixtureWithScope<?>> F fixture( F fixture ) {
        this.fixtures.add( fixture.withScope( scope ) );

        return fixture;
    }

    @SuppressWarnings( "unchecked" )
    public T withScope( Scope scope ) {
        this.scope = scope;

        fixtures.forEach( f -> f.withScope( scope ) );

        return ( T ) this;
    }

    public final Scope getScope() {
        return scope;
    }

    @Override
    public void beforeSuite() {
        if( scope == Scope.SUITE ) {
            beforeAll();
        }
    }

    @Override
    public void afterSuite() {
        if( scope == Scope.SUITE ) {
            afterAll();
        }
    }

    @Override
    public void beforeClass() {
        if( scope == Scope.CLASS ) {
            beforeAll();
        }
    }

    @Override
    public void afterClass() {
        if( scope == Scope.CLASS ) {
            afterAll();
        }
    }

    @Override
    public void beforeMethod() {
        if( scope == Scope.METHOD ) {
            beforeAll();
        }
    }

    @Override
    public void afterMethod() {
        if( scope == Scope.METHOD ) {
            afterAll();
        }
    }

    protected void beforeAll() {
        fixtures.forEach( FixtureWithScope::before );

        before();
    }

    protected void afterAll() {
        after();

        Lists.reverse( fixtures ).forEach( FixtureWithScope::after );
    }

    protected abstract void before();

    protected abstract void after();
}
