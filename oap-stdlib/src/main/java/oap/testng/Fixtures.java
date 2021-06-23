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

import com.mchange.util.AssertException;
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

    public static <F extends Fixture> F suiteFixture( F fixture ) {
        var ret = suiteFixtures.putIfAbsent( fixture.getClass(), fixture );
        if( ret != null ) throw new AssertException( "fixture is already registered" );
        return fixture;
    }

    public <F extends Fixture> F fixture( F fixture ) {
        return fixture( Position.LAST, fixture );
    }

    public <F extends Fixture> F fixture( Position position, F fixture ) {
        if( fixture instanceof AbstractScopeFixture<?> && ( ( AbstractScopeFixture<?> ) fixture ).getScope() == SUITE )
            throw new AssertException( "use static Fixtures#suiteFixture" );
        else {
            if( position == Position.FIRST ) fixtures.addFirst( fixture );
            else fixtures.addLast( fixture );

            return fixture;
        }
    }

    @BeforeSuite
    public void fixBeforeSuite() {
        suiteFixtures.values().forEach( Fixture::beforeSuite );
    }

    @AfterSuite
    public void fixAfterSuite() {
        Lists.reverse( suiteFixtures.values() ).forEach( Fixture::afterSuite );
    }

    @BeforeClass
    public void fixBeforeClass() {
        suiteFixtures.values().forEach( Fixture::beforeClass );
        fixtures.forEach( Fixture::beforeClass );
    }

    @AfterClass
    public void fixAfterClass() {
        fixtures.descendingIterator().forEachRemaining( Fixture::afterClass );
        Lists.reverse( suiteFixtures.values() ).forEach( Fixture::afterClass );
    }

    @BeforeMethod
    public void fixBeforeMethod() {
        suiteFixtures.values().forEach( Fixture::beforeMethod );
        fixtures.forEach( Fixture::beforeMethod );
    }

    @AfterMethod
    public void fixAfterMethod() {
        fixtures.descendingIterator().forEachRemaining( Fixture::afterMethod );
        Lists.reverse( suiteFixtures.values() ).forEach( Fixture::afterMethod );
    }

    public enum Position {
        FIRST, LAST
    }
}
