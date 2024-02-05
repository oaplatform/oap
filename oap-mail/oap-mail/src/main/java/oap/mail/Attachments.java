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

import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;

import javax.activation.FileDataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.URLDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class Attachments {

    static String makeMimeType( Attachment attachment ) throws MessagingException {
        String name = attachment.getName();
        if( name == null ) {
            // try to construct the name from filename
            name = attachment.getFile();
            if( name != null ) {
                int p = name.lastIndexOf( '/' );
                if( p < 0 ) p = name.lastIndexOf( '\\' );
                if( p >= 0 ) name = name.substring( p + 1 );
            }
        }
        try {
            MimeType mt = new MimeType( attachment.getContentType() );
            if( name != null ) {
                String encoded = name;
                try {
                    encoded = new QCodec().encode( name, "UTF-8" );
                    String sub = encoded.substring( 10, encoded.length() - 2 );
                    if( sub.equals( name ) ) encoded = name;
                } catch( EncoderException e ) {
                    log.warn( "encoding error for: " + name, e );
                }
                mt.setParameter( "name", encoded );
            }
            if( attachment.getFile() == null ) mt.setParameter( "charset", "UTF-8" );
            return mt.toString();
        } catch( MimeTypeParseException e ) {
            throw new MessagingException( "bad content type: " + attachment.getContentType(), e );
        }
    }

    @Slf4j
    static class HtmlMimeMultipart extends MimeMultipart {

        HtmlMimeMultipart() {
            try {
                this.setSubType( "related" );
            } catch( MessagingException e ) {
                log.warn( "Cannot set 'related' sub type", e );
            }
        }

        public String getContentType() {
            try {
                MimeType mt = new MimeType( super.getContentType() );
                mt.setParameter( "type", "text/html" );
                return mt.toString();
            } catch( MimeTypeParseException e ) {
                log.warn( "Cannot get content type from: {}", super.getContentType(), e );
                return super.getContentType();
            }
        }
    }

    static class MimeFileDataSource extends FileDataSource {
        String mimeType;

        MimeFileDataSource( Attachment attachment ) throws MessagingException {
            super( attachment.getFile() );
            mimeType = makeMimeType( attachment );
        }

        public String getContentType() {
            return mimeType != null ? mimeType : super.getContentType();
        }
    }

    static class MimeURLDataSource extends URLDataSource {
        String mimeType;

        MimeURLDataSource( Attachment attachment ) throws MessagingException, MalformedURLException {
            super( new URL( attachment.getFile() ) );
            mimeType = makeMimeType( attachment );
        }

        public String getContentType() {
            return mimeType != null ? mimeType : super.getContentType();
        }
    }

    static class ClasspathDataSource extends URLDataSource {
        String mimeType;

        ClasspathDataSource( Attachment attachment ) throws MessagingException {
            super( Resources.url( ClasspathDataSource.class, attachment.getFile().substring( "classpath://".length() ) ).orElseThrow() );
            mimeType = makeMimeType( attachment );
        }

        public String getContentType() {
            return mimeType != null ? mimeType : super.getContentType();
        }
    }
}
