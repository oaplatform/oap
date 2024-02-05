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
import oap.util.Lists;
import oap.util.Stream;
import org.assertj.core.api.AbstractIterableAssert;

import javax.annotation.Nonnull;
import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.fail;

public final class MessagesAssertion extends AbstractIterableAssert<MessagesAssertion, Iterable<? extends Message>, Message, MessageAssertion> {

    private MessagesAssertion( @Nonnull Iterable<? extends Message> messages ) {
        super( messages, MessagesAssertion.class );
    }

    @Nonnull
    public static MessagesAssertion assertMessages( @Nonnull Iterable<? extends Message> messages ) {
        return new MessagesAssertion( messages );
    }

    @SneakyThrows( MessagingException.class )
    public static MessagesAssertion assertInbox( String user, String password ) {
        try( Folder inbox = MailBox.connectToInbox( user, password ) ) {
            return new MessagesAssertion( MailBox.getMessagesFromBox( inbox ) );
        }
    }

    @Nonnull
    public MessagesAssertion sentTo( @Nonnull String to, @Nonnull Consumer<Message> assertion ) {
        return by( m -> Lists.contains( m.to, ma -> ma.mail.equals( to ) ),
            assertion, "can't find message sent to " + to );
    }

    @Nonnull
    public MessagesAssertion bySubject( @Nonnull String subject, @Nonnull Consumer<Message> assertion ) {
        return by( m -> subject.equals( m.subject ),
            assertion, "can't find message with subject " + subject );
    }

    @Nonnull
    public MessagesAssertion by( @Nonnull Predicate<Message> predicate, @Nonnull Consumer<Message> assertion ) {
        return by( predicate, assertion, "can't find message" );
    }

    @Nonnull
    public MessagesAssertion by( @Nonnull Predicate<Message> predicate, @Nonnull Consumer<Message> assertion, @Nonnull String failureMessage ) {
        Stream.of( this.actual.iterator() )
            .filter( predicate )
            .findAny()
            .ifPresentOrElse( assertion, () -> fail( failureMessage ) );
        return this;
    }

    @Override
    protected MessageAssertion toAssert( Message message, String s ) {
        return MessageAssertion.assertMessage( message );
    }

    @Override
    protected MessagesAssertion newAbstractIterableAssert( Iterable<? extends Message> iterable ) {
        return assertMessages( iterable );
    }
}
