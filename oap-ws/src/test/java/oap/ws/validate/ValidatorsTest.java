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

package oap.ws.validate;

import oap.reflect.Reflect;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidatorsTest {
    @Test
    public void caching() throws Exception {
        Method method = Validatee.class.getMethod( "m", String.class );
        Validators.Validator v1 = Validators.forMethod( Reflect.reflect( Validatee.class ).method( method ).orElseThrow(),
            new Validatee(), false );
        Validators.Validator v2 = Validators.forMethod( Reflect.reflect( Validatee.class ).method( method ).orElseThrow(),
            new Validatee(), false );

        assertThat( v1 ).isNotSameAs( v2 );
    }

    public static class Validatee {
        @WsValidate( "validate" )
        public void m( String a ) { }

        public ValidationErrors validate( String a ) {
            return ValidationErrors.empty();
        }
    }
}
