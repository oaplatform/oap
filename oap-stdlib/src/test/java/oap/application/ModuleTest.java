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

package oap.application;

import org.testng.annotations.Test;

import java.util.Map;

import static oap.testng.Asserts.assertString;

/**
 * Created by igor.petrenko on 2021-02-26.
 */
public class ModuleTest {
    @Test
    public void testReference() {
        var reference = Module.Reference.of( "@service:test-module:test-service" );
        assertString( reference.service ).isEqualTo( "test-service" );
        assertString( reference.module ).isEqualTo( "test-module" );
    }

    @Test
    public void testReferenceObject() {
        var reference = Module.Reference.of( Map.of( "module", "test-module", "service", "test-service" ) );
        assertString( reference.service ).isEqualTo( "test-service" );
        assertString( reference.module ).isEqualTo( "test-module" );
    }

    @Test
    public void testReferenceWithoutPrefix() {
        var reference = Module.Reference.of( "test-module:test-service", "this" );
        assertString( reference.service ).isEqualTo( "test-service" );
        assertString( reference.module ).isEqualTo( "test-module" );
    }

    @Test
    public void testReferenceWithoutModuleAndPrefix() {
        var reference = Module.Reference.of( "test-service", "this" );
        assertString( reference.service ).isEqualTo( "test-service" );
        assertString( reference.module ).isEqualTo( "this" );
    }
}
