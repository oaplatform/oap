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

import java.util.ArrayList;

import static oap.testng.AbstractFixture.Scope.SUITE;


@Slf4j
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Fixtures {
    private static final ArrayList<AbstractFixture<?>> suiteFixtures = new ArrayList<>();
    private final ArrayList<AbstractFixture<?>> fixtures = new ArrayList<>();

    public static <F extends AbstractFixture<?>> F suiteFixture( F fixture ) throws IllegalArgumentException {
        fixture.scope = SUITE;

        suiteFixtures.add( fixture );

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

    public synchronized <F extends AbstractFixture<?>> F fixture( F fixture ) throws IllegalCallerException {
        fixtures.add( fixture );

        return fixture;
    }

    @BeforeSuite
    public void fixBeforeSuite() {
        suiteFixtures.forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeSuite ) );
    }

    @AfterSuite( alwaysRun = true )
    public void fixAfterSuite() {
        SilentRun silentRun = new SilentRun();
        Lists.reverse( suiteFixtures ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> {
            log.info( "afterSuite {}", f.getClass() );
            silentRun.run( f::afterSuite );
        } ) );
        silentRun.done();
    }

    @BeforeClass
    public void fixBeforeClass() {
        suiteFixtures.forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeClass ) );
        fixtures.forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeClass ) );
    }

    @AfterClass( alwaysRun = true )
    public void fixAfterClass() {
        SilentRun silentRun = new SilentRun();
        Lists.reverse( fixtures ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterClass ) ) );
        Lists.reverse( suiteFixtures ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterClass ) ) );
        silentRun.done();
    }

    @BeforeMethod
    public void fixBeforeMethod() {
        suiteFixtures.forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeMethod ) );
        fixtures.forEach( f -> Threads.withThreadName( f.toThreadName(), f::beforeMethod ) );
    }

    @AfterMethod( alwaysRun = true )
    public void fixAfterMethod() {
        SilentRun silentRun = new SilentRun();
        Lists.reverse( fixtures ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterMethod ) ) );
        Lists.reverse( suiteFixtures ).forEach( f -> Threads.withThreadName( f.toThreadName(), () -> silentRun.run( f::afterMethod ) ) );
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
