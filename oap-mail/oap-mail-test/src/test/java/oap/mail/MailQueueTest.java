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

import oap.mail.test.MessageAssertion;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Lists;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.mail.test.MessagesAssertion.assertMessages;
import static oap.util.function.Functions.empty.accept;
import static oap.util.function.Functions.empty.reject;
import static org.assertj.core.api.Assertions.assertThat;

public class MailQueueTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public MailQueueTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void persist() {
        Path location = testDirectoryFixture.testPath( "queue" );
        MailQueue queue = prepareQueue( location );
        queue.processing( reject() );
        assertThat( location.resolve( "mail.gz" ) ).exists();
        MailQueue queue2 = new MailQueue( new MailQueuePersistenceFile( location ) );
        assertMessages( queue2.messages() )
            .hasSize( 2 )
            .bySubject( "subj1", MessageAssertion::assertMessage )
            .bySubject( "subj2", MessageAssertion::assertMessage );

        queue2.processing( accept() );
        assertMessages( queue2.messages() ).isEmpty();
        MailQueue queue3 = new MailQueue( new MailQueuePersistenceFile( location ) );
        assertMessages( queue3.messages() ).isEmpty();
    }

    private MailQueue prepareQueue( Path location ) {
        MailQueue queue = new MailQueue( new MailQueuePersistenceFile( location ) );
        queue.add( new Message( "subj1", "body", Lists.empty() ) );
        queue.add( new Message( "subj2", "body", Lists.empty() ) );
        Message message = new Message( "subj3", "body", Lists.empty() );
        message.created = DateTime.now().minus( Dates.w( 3 ) );
        queue.add( message );
        return queue;
    }
}
