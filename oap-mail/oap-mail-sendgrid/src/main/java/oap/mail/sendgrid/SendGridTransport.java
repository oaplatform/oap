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

package oap.mail.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import oap.mail.MailAddress;
import oap.mail.Message;
import oap.mail.Transport;

import javax.mail.Part;

@SuppressWarnings( "unused" )
@Slf4j
public class SendGridTransport implements Transport {
    private final String sendGridKey;

    public SendGridTransport( String sendGridKey ) {
        this.sendGridKey = sendGridKey;
    }

    @Override
    public void send( Message message ) {
        Email from = new Email( message.from.toString() );
        Content content = new Content( "text/html", message.body );
        SendGrid sendGrid = new SendGrid( sendGridKey );
        Request request = new Request();
        request.setMethod( Method.POST );
        request.setEndpoint( "mail/send" );
        for( MailAddress address : message.to ) {
            Email to = new Email( address.toString() );
            Mail mail = new Mail( from, message.subject, to, content );
            message.attachments.stream()
                .map( this::createAttachments )
                .forEach( mail::addAttachments );
            try {
                request.setBody( mail.build() );
                sendGrid.api( request );
            } catch( Exception e ) {
                log.error( "failed to send {}", message, e );
            }
        }
    }

    private Attachments createAttachments( oap.mail.Attachment oapAttachment ) {
        Attachments attachments = new Attachments();
        attachments.setContent( oapAttachment.getContent() );
        attachments.setContentId( oapAttachment.getContentId() );
        attachments.setDisposition( Part.ATTACHMENT );
        attachments.setFilename( oapAttachment.getFile() );
        attachments.setType( oapAttachment.getContentType() );

        return attachments;
    }
}
