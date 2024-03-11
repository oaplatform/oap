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

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.util.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import static oap.testng.AbstractFixture.Scope.SUITE;


@Slf4j
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Fixtures {
    private static final LinkedHashMap<String, AbstractFixture<?>> suiteFixtures = new LinkedHashMap<>();
    private final LinkedHashMap<String, AbstractFixture<?>> fixtures = new LinkedHashMap<>();

    public static <F extends AbstractFixture<?>> F suiteFixture( F fixture ) throws IllegalArgumentException {
        fixture.withScope( SUITE );

        var ret = suiteFixtures.put( fixture.prefix, fixture );
        if( ret != null )
            throw new IllegalArgumentException( "Fixture '" + fixture.getClass().getCanonicalName() + "' has already been registered, registered: " + suiteFixtures.keySet() );
        return fixture;
    }

    public static Fixtures fixtures( AbstractFixture<?>... fixtures ) {
        return new Fixtures() {
            {
                for( var f : fixtures ) {
                    fixture( f );
                }
            }
        };
    }

    public Set<String> nameSet() {
        var ret = new HashSet<String>();
        ret.addAll( suiteFixtures.keySet() );
        ret.addAll( fixtures.keySet() );
        return ret;
    }

    public synchronized <F extends AbstractFixture<?>> F fixture( F fixture ) throws IllegalCallerException {
        if( nameSet().contains( fixture.prefix ) ) {
            throw new IllegalArgumentException( "Fixture '" + fixture.prefix + "/" + fixture.getClass().getCanonicalName() + "' has already been registered, registered: " + nameSet() );
        }

        fixtures.put( fixture.prefix, fixture );

        return fixture;
    }

    @BeforeSuite
    public void fixBeforeSuite() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::initialize ) );

        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeSuite ) );
    }

    @AfterSuite( alwaysRun = true )
    public void fixAfterSuite() {
        SilentRun silentRun = new SilentRun();
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterSuite ) ) );

        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::shutdown ) ) );
        silentRun.done();
    }

    @BeforeClass
    public void fixBeforeClass() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::initialize ) );
        fixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::initialize ) );

        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeClass ) );
        fixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeClass ) );
    }

    @AfterClass( alwaysRun = true )
    public void fixAfterClass() {
        SilentRun silentRun = new SilentRun();

        Lists.reverse( fixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterClass ) ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterClass ) ) );

        Lists.reverse( fixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::shutdown ) ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::shutdown ) ) );

        silentRun.done();
    }

    @BeforeMethod
    public void fixBeforeMethod() {
        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::initialize ) );
        fixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::initialize ) );

        suiteFixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeMethod ) );
        fixtures.values().forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeMethod ) );
    }

    @AfterMethod( alwaysRun = true )
    public void fixAfterMethod() {
        SilentRun silentRun = new SilentRun();

        Lists.reverse( fixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterMethod ) ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterMethod ) ) );

        Lists.reverse( fixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::shutdown ) ) );
        Lists.reverse( suiteFixtures.values() ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::shutdown ) ) );

        silentRun.done();
    }

    private static class SilentRun {
        private Throwable throwable;

        public void run( Runnable run ) {
            try {
                run.run();
            } catch( Throwable e ) {
                if( throwable == null ) throwable = e;

                log.error( e.getMessage(), e );
            }
        }

        public void done() {
            if( throwable != null ) throw new RuntimeException( throwable );
        }
    }
}
