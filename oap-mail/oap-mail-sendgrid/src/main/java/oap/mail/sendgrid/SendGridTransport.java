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

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.mail.MailAddress;
import oap.mail.Message;
import oap.mail.Transport;

import javax.mail.Part;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SendGridTransport implements Transport {
    private final String sendGridKey;

    public SendGridTransport( String sendGridKey ) {
        this.sendGridKey = sendGridKey;
    }

    @Override
    public void send( Message message ) {
        try( HttpClient client = HttpClient.newHttpClient() ) {
            for( MailAddress address : message.to ) {
                try {
                    var request = HttpRequest.newBuilder( URI.create( "https://api.sendgrid.com/v3/mail/send" ) )
                        .header( "Authorization", "Bearer " + sendGridKey )
                        .header( "Content-Type", "application/json" )
                        .POST( HttpRequest.BodyPublishers.ofString( Binder.json.marshal( payload( message, address ) ) ) ).build();
                    var response = client.send( request, HttpResponse.BodyHandlers.discarding() );
                    if( response.statusCode() >= 400 ) log.error( "failed to send {}: HTTP {}", message, response.statusCode() );
                } catch( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    log.error( "failed to send {}", message, e );
                    return;
                } catch( Exception e ) {
                    log.error( "failed to send {}", message, e );
                }
            }
        }
    }

    static Map<String, Object> payload( Message message, MailAddress address ) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put( "from", Map.of( "email", message.from.toString() ) );
        payload.put( "subject", message.subject );
        payload.put( "personalizations", List.of( Map.of( "to", List.of( Map.of( "email", address.toString() ) ) ) ) );
        var content = new LinkedHashMap<String, Object>();
        content.put( "type", "text/html" );
        content.put( "value", message.body );
        content.values().removeIf( value -> value == null || "".equals( value ) );
        payload.put( "content", List.of( content ) );
        if( !message.attachments.isEmpty() ) payload.put( "attachments", message.attachments.stream().map( attachment -> {
            var fields = new LinkedHashMap<String, Object>();
            fields.put( "content", attachment.getContent() );
            fields.put( "content_id", attachment.getContentId() );
            fields.put( "disposition", Part.ATTACHMENT );
            fields.put( "filename", attachment.getFile() );
            fields.put( "type", attachment.getContentType() );
            fields.values().removeIf( value -> value == null || "".equals( value ) );
            return fields;
        } ).toList() );
        payload.values().removeIf( value -> value == null || "".equals( value ) );
        return payload;
    }
}
