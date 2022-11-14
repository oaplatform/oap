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

import oap.concurrent.Threads;
import oap.util.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import static oap.testng.Fixture.Scope.SUITE;


@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Fixtures {
    private static final LinkedHashMap<Class<? extends Fixture>, Fixture> suiteFixtures = new LinkedHashMap<>();
    private final LinkedList<Fixture> fixtures = new LinkedList<>();

    @SuppressWarnings( { "unchecked", "checkstyle:ParameterAssignment" } )
    public static <F extends Fixture> F suiteFixture( F fixture ) throws IllegalArgumentException {
        if( fixture instanceof AbstractScopeFixture<?> ) fixture = ( F ) ( ( AbstractScopeFixture<?> ) fixture ).withScope( SUITE );

        var ret = suiteFixtures.putIfAbsent( fixture.getClass(), fixture );
        if( ret != null ) throw new IllegalArgumentException( "Fixture '" + fixture.getClass().getCanonicalName() + "' has already been registered, registered: " + suiteFixtures.keySet() );
        return fixture;
    }

    public <F extends Fixture> F fixture( F fixture ) throws IllegalCallerException {
        return fixture( Position.LAST, fixture );
    }

    public <F extends Fixture> F fixture( Position position, F fixture ) throws IllegalCallerException {
        if( fixture instanceof AbstractScopeFixture<?> && ( ( AbstractScopeFixture<?> ) fixture ).getScope() == SUITE )
            throw new IllegalCallerException( "use static Fixtures#suiteFixture" );
        else {
            if( position == Position.FIRST ) fixtures.addFirst( fixture );
            else fixtures.addLast( fixture );

            return fixture;
        }
    }

    @BeforeSuite
    public void fixBeforeSuite() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.getUniqueName(), f::beforeSuite ) );
    }

    @AfterSuite
    public void fixAfterSuite() {
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.getUniqueName(), f::afterSuite ) );
    }

    @BeforeClass
    public void fixBeforeClass() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.getUniqueName(), f::beforeClass ) );
        fixtures.forEach( f -> Threads.withThreadName( f.getUniqueName(), f::beforeClass ) );
    }

    @AfterClass
    public void fixAfterClass() {
        fixtures.descendingIterator().forEachRemaining( f -> Threads.withThreadName( f.getUniqueName(), f::afterClass ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.getUniqueName(), f::afterClass ) );
    }

    @BeforeMethod
    public void fixBeforeMethod() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.getUniqueName(), f::beforeMethod ) );
        fixtures.forEach( f -> Threads.withThreadName( f.getUniqueName(), f::beforeMethod ) );
    }

    @AfterMethod
    public void fixAfterMethod() {
        fixtures.descendingIterator().forEachRemaining( f -> Threads.withThreadName( f.getUniqueName(), f::afterMethod ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.getUniqueName(), f::afterMethod ) );
    }

    protected Fixture obtainRegistered( Class<? extends Fixture> clazz ) {
        return suiteFixtures.get( clazz );
    }

    public enum Position {
        FIRST, LAST
    }
}
