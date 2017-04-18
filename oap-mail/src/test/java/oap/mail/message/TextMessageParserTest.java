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

package oap.mail.message;

import oap.mail.Message;
import org.testng.annotations.Test;

import static oap.testng.Asserts.contentOfTestResource;
import static org.testng.Assert.assertEquals;

public class TextMessageParserTest {
    @Test
    public void subject() {
        Message message = new TextMessageParser().parse( contentOfTestResource( getClass(), "subject.mail" ) );
        assertEquals( "Subject", message.getSubject() );
        assertEquals( "Body", message.getBody() );
    }

    @Test
    public void attachment() {
        Message message = new TextMessageParser().parse( contentOfTestResource( getClass(), "att1.mail" ) );
        assertEquals( "Subject", message.getSubject() );
        assertEquals( "Body", message.getBody() );
        assertEquals( "Attachment1", message.getAttachments().get( 0 ).getContent() );
    }

    @Test
    public void multiAttachment() {
        Message message = new TextMessageParser().parse( contentOfTestResource( getClass(), "att2.mail" ) );
        assertEquals( "Subject", message.getSubject() );
        assertEquals( "Body", message.getBody() );
        assertEquals( "Attachment1", message.getAttachments().get( 0 ).getContent() );
        assertEquals( "Attachment2", message.getAttachments().get( 1 ).getContent() );
        assertEquals( "Att\nachm\nent3", message.getAttachments().get( 2 ).getContent() );
    }
}