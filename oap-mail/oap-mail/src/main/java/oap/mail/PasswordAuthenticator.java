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

package oap.mail;

import lombok.ToString;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

@ToString( exclude = { "password" } )
public class PasswordAuthenticator extends Authenticator {
    public final String username;
    public final String password;

    public PasswordAuthenticator( String username, String password ) {
        super();
        this.username = username;
        this.password = password;
    }

    public PasswordAuthenticator( URL config ) {
        this( oap.util.Properties.read( config ) );
    }

    public PasswordAuthenticator( Path config ) {
        this( oap.util.Properties.read( config ) );
    }

    public PasswordAuthenticator( Properties config ) {
        this( config.getProperty( "username" ), config.getProperty( "password" ) );
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication( username, password );
    }
}
