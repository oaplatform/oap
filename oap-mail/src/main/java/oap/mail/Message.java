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

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String subject;
    private String body;
    private List<Attachment> attachments = new ArrayList<Attachment>();
    private MailAddress from;
    private MailAddress[] to = new MailAddress[0];
    private MailAddress[] cc = new MailAddress[0];
    private MailAddress[] bcc = new MailAddress[0];
    private String contentType = "text/plain";

    public Message( String subject, String body, List<Attachment> attachments ) {
        this.body = body;
        this.subject = subject;
        this.attachments.addAll( attachments );
    }

    public Message() {
    }

    public void setFrom( MailAddress from ) {
        this.from = from;
    }

    public void setTo( MailAddress... to ) {
        this.to = to;
    }

    public void setCc( MailAddress... cc ) {
        this.cc = cc;
    }

    public void setBcc( MailAddress... bcc ) {
        this.bcc = bcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public MailAddress getFrom() {
        return from;
    }

    public MailAddress[] getCc() {
        return cc;
    }

    public MailAddress[] getBcc() {
        return bcc;
    }

    public MailAddress[] getTo() {
        return to;
    }

    public void setSubject( String subject ) {
        this.subject = subject;
    }

    public void setBody( String body ) {
        this.body = body;
    }

    public void setContentType( String contentType ) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void addAttachment( Attachment attachment ) {
        attachments.add( attachment );
    }

    public final class Mime {
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_PNG = "image/png";

        private Mime() {
        }
    }
}
