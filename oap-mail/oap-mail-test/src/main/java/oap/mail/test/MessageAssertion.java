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

package oap.mail.test;

import lombok.SneakyThrows;
import oap.mail.Message;
import org.assertj.core.api.AbstractAssert;

import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.Map;

import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public final class MessageAssertion extends AbstractAssert<MessageAssertion, Message> {
    private MessageAssertion( Message message ) {
        super( message, MessageAssertion.class );
    }

    public static MessageAssertion assertMessage( Message message ) {
        return new MessageAssertion( message );
    }

    @SneakyThrows( MessagingException.class )
    public static MessageAssertion assertInboxMostRecentMessage( String mail, String password ) {
        try( Folder inbox = MailBox.connectToInbox( mail, password ) ) {
            return new MessageAssertion( MailBox.getLastSentMessageFromTheBox( inbox ) );
        }
    }

    public MessageAssertion isFrom( String email ) {
        assertString( this.actual.from.mail ).isEqualTo( email );
        return this;
    }

    public MessageAssertion isSentTo( String... emails ) {
        assertThat( this.actual.to )
            .extracting( ma -> ma.mail )
            .contains( emails );
        return this;
    }

    public MessageAssertion isCopiedTo( String... emails ) {
        assertThat( this.actual.cc )
            .extracting( ma -> ma.mail )
            .contains( emails );
        return this;
    }

    public MessageAssertion isBlindlyCopiedTo( String... emails ) {
        assertThat( this.actual.bcc )
            .extracting( ma -> ma.mail )
            .contains( emails );
        return this;
    }

    public MessageAssertion hasSubject( String subject ) {
        assertString( this.actual.subject ).isEqualTo( subject );
        return this;
    }

    public MessageAssertion hasContentType( String contentType ) {
        assertThat( this.actual.contentType ).isEqualTo( contentType );
        return this;
    }

    public MessageAssertion hasBody( String body ) {
        assertString( this.actual.body ).isEqualTo( body );
        return this;
    }

    public MessageAssertion hasBody( Class<?> contextClass, String resource ) {
        return hasBody( contextClass, resource, Map.of() );
    }

    public MessageAssertion hasBody( Class<?> contextClass, String resource, Map<String, Object> substitutions ) {
        return hasBody( contentOfTestResource( contextClass, resource, substitutions ) );
    }
}
