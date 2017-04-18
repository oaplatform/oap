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

import lombok.extern.slf4j.Slf4j;
import oap.mail.Attachment;
import oap.mail.Message;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@Slf4j
public class SaxMessageParser extends DefaultHandler {
    private Message message;
    private String characters = "";
    private Attachment attachment;

    public void startDocument() throws SAXException {
        message = new Message();
    }

    public void characters( char ch[], int start, int length ) throws SAXException {
        characters += new String( ch, start, length );
    }

    public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException {
        characters = "";
        if( "body".equals( qName ) ) message.setContentType( attributes.getValue( "type" ) );
        if( "attachment".equals( qName ) ) {
            attachment = new Attachment( attributes.getValue( "type" ), null,
                attributes.getValue( "content-id" ), attributes.getValue( "file" ),
                attributes.getValue( "name" ) );
        }
    }

    public void endElement( String uri, String localName, String qName ) throws SAXException {
        if( "subject".equals( qName ) ) message.setSubject( characters );
        if( "body".equals( qName ) ) message.setBody( characters );
        if( "attachment".equals( qName ) ) {
            if( attachment.getFile() == null )
                attachment.setContent( characters );
            message.addAttachment( attachment );
        }
    }

    public void warning( SAXParseException e ) throws SAXException {
        log.warn( e.toString() );
    }

    public Message getMessage() {
        return message;
    }

}
