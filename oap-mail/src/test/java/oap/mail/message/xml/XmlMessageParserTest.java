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
package oap.mail.message.xml;


import oap.mail.Attachment;
import oap.mail.Message;
import org.testng.annotations.Test;

import static oap.testng.Asserts.contentOfTestResource;
import static org.testng.Assert.assertEquals;

public class XmlMessageParserTest {
    @Test
    public void parse() {
        Message message = new XmlMessageParser().parse( contentOfTestResource( getClass(), "simple.xmail" ) );
        assertEquals( "subject", message.getSubject() );
        assertEquals( "body", message.getBody() );
        assertEquals( "text/plain", message.getContentType() );
    }

    @Test
    public void attachment() {
        Message message = new XmlMessageParser().parse( contentOfTestResource( getClass(), "text-attachment.xmail" ) );
        assertEquals( "subject", message.getSubject() );
        assertEquals( "body", message.getBody() );
        assertEquals( 1, message.getAttachments().size() );
        Attachment attachment = message.getAttachments().get( 0 );
        assertEquals( "text/plain", attachment.getContentType() );
        assertEquals( "this is text attachment", attachment.getContent() );
    }
}
