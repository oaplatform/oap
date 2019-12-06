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

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.util.LinkedList;

public class Fixtures {
    private final LinkedList<Fixture> fixtures = new LinkedList<>();

    public void fixture( Fixture fixture ) {
        fixture( Position.LAST, fixture );
    }

    public void fixture( Position position, Fixture fixture ) {
        switch( position ) {
            case FIRST -> fixtures.addFirst( fixture );
            case LAST -> fixtures.addLast( fixture );
        }
    }

    @BeforeClass
    public void fixBeforeClass() {
        fixtures.iterator().forEachRemaining( Fixture::beforeClass );
    }

    @AfterClass
    public void fixAfterClass() {
        fixtures.descendingIterator().forEachRemaining( Fixture::afterClass );
    }

    @BeforeMethod
    public void fixBeforeMethod() {
        fixtures.iterator().forEachRemaining( Fixture::beforeMethod );
    }

    @AfterMethod
    public void fixAfterMethod() {
        fixtures.descendingIterator().forEachRemaining( Fixture::afterMethod );
    }

    enum Position {
        FIRST, LAST
    }
}
