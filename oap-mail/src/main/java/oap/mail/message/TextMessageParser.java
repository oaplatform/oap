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

import oap.mail.Attachment;
import oap.mail.Message;

import java.util.ArrayList;

public class TextMessageParser implements MessageParser {
    public static final String SUBJECT = "--subject--";
    public static final String BODY = "--body--";
    public static final String ATTACHMENT = "--attachment--";

    public Message parse( String content ) {
        String[] values = content.split( "[\n\r]*--body--[\n\r]*|[\n\r]*--subject--[\n\r]*|[\n\r]*--attachment--[\n\r]*" );

        ArrayList<Attachment> attachments = new ArrayList<>();
        for( int i = 3; i < values.length; i++ )
            attachments.add( new Attachment( Message.Mime.TEXT_PLAIN, values[i] ) );
        return new Message( values[1], values[2], attachments );
    }
}
