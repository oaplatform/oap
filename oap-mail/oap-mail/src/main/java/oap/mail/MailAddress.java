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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@ToString
@EqualsAndHashCode
public class MailAddress {
    public final String personal;
    public final String mail;

    public MailAddress( String mail ) {
        this( null, mail );
    }

    @JsonCreator
    public MailAddress( String personal, String mail ) {
        this.personal = personal;
        this.mail = mail;
    }

    public static MailAddress[] of( String... mails ) {
        MailAddress[] result = new MailAddress[mails.length];
        for( int i = 0; i < mails.length; i++ ) result[i] = new MailAddress( mails[i] );
        return result;
    }

    public static MailAddress of( InternetAddress address ) {
        return new MailAddress( address.getPersonal(), address.getAddress() );
    }

    public static List<MailAddress> of( InternetAddress[] addresses ) {
        if( ArrayUtils.isEmpty( addresses ) ) {
            return Collections.emptyList();
        }
        return Stream.of( addresses ).map( MailAddress::of ).toList();
    }

    public static MailAddress of( String personal, String address ) {
        return new MailAddress( personal, address );
    }

    @SneakyThrows
    public InternetAddress toInternetAddress() {
        return personal == null
            ? new InternetAddress( mail )
            : new InternetAddress( mail, personal, UTF_8.toString() );
    }

    public String toString() {
        return personal == null ? mail : "\"" + personal + "\" <" + mail + ">";
    }

}
