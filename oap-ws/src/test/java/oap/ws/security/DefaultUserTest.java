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

package oap.ws.security;

import lombok.val;
import oap.json.Binder;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 31.10.2017.
 */
public class DefaultUserTest {
    @Test
    public void testJson() {
        val user = new DefaultUser( Role.ADMIN, "orgid", "my@enai.com" );
        user.password = "test1";
        user.organizationName = "irg name";

        val json = Binder.json.marshal( user );
        assertThat( json ).isEqualTo( "{\"email\":\"my@enai.com\",\"password\":\"test1\",\"role\":\"ADMIN\",\"organizationId\":\"orgid\",\"organizationName\":\"irg name\"}" );
        val clone = Binder.json.<DefaultUser>unmarshal( DefaultUser.class, json );

        assertThat( clone.organizationId ).isEqualTo( "orgid" );
        assertThat( clone.organizationName ).isEqualTo( "irg name" );
        assertThat( clone.password ).isEqualTo( "test1" );
        assertThat( clone.role ).isEqualTo( Role.ADMIN );
        assertThat( clone.email ).isEqualTo( "my@enai.com" );
    }

}
