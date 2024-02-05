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
import lombok.ToString;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

@ToString( of = { "subject", "from", "to", "cc", "bcc", "created" } )
public class Message {
    public String subject;
    public String body;
    public List<Attachment> attachments = new ArrayList<>();
    public MailAddress from;
    public final List<MailAddress> to = new ArrayList<>();
    public final List<MailAddress> cc = new ArrayList<>();
    public final List<MailAddress> bcc = new ArrayList<>();
    public String contentType = "text/plain";
    public DateTime created = new DateTime();

    @JsonCreator
    public Message( String subject, String body, List<Attachment> attachments ) {
        this.body = body;
        this.subject = subject;
        if( attachments != null ) this.attachments.addAll( attachments );
    }

    public Message() {
        this( null, null, List.of() );
    }
}
