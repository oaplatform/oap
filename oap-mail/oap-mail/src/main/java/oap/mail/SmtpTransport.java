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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.util.Strings;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ToString
@Slf4j
public class SmtpTransport implements oap.mail.Transport {
    static {
        MailcapCommandMap mc = ( MailcapCommandMap ) CommandMap.getDefaultCommandMap();
        mc.addMailcap( "text/html;; x-java-content-handler=com.sun.mail.handlers.text_html" );
        mc.addMailcap( "text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml" );
        mc.addMailcap( "text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain" );
        mc.addMailcap( "multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed" );
        mc.addMailcap( "message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822" );
        CommandMap.setDefaultCommandMap( mc );
    }

    public final String host;
    public final int port;
    public final boolean tls;
    public final Authenticator authenticator;
    private final Properties properties = new Properties();

    public SmtpTransport( String host, int port, boolean tls, Authenticator authenticator ) {
        this( host, port, tls, authenticator, "TLSv1.2" );
    }

    public SmtpTransport( String host, int port, boolean tls, Authenticator authenticator, String tlsVersion ) {
        this.host = host;
        this.port = port;
        this.tls = tls;
        this.authenticator = authenticator;
        properties.put( "mail.smtp.host", host );
        properties.put( "mail.smtp.port", String.valueOf( port ) );
        properties.put( "mail.smtp.starttls.enable", String.valueOf( tls ) );
        if( tls ) {
            properties.put( "mail.smtp.ssl.protocols", tlsVersion );
        }
        properties.put( "mail.smtp.auth", String.valueOf( authenticator != null ) );
    }

    public void send( Message message ) {
        log.debug( "sending {}", message );
        Session session = Session.getInstance( properties, authenticator );
        MimeMessage mimeMessage = new MimeMessage( session );
        try {
            mimeMessage.setFrom( message.from.toInternetAddress() );
            for( MailAddress recipient : message.to )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.TO, recipient.toInternetAddress() );
            for( MailAddress recipient : message.cc )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.CC, recipient.toInternetAddress() );
            for( MailAddress recipient : message.bcc )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.BCC, recipient.toInternetAddress() );
            mimeMessage.setSubject( Strings.toQuotedPrintable( message.subject ) );
            mimeMessage.setHeader( "Content-Transfer-Encoding", "quoted-printable" );
            if( message.attachments.isEmpty() ) {
                mimeMessage.setContent( message.body, message.contentType + "; charset=UTF-8" );
            } else {
                HashSet<String> cidIds = new HashSet<>();
                MimeMultipart multipart;
                if( "text/html".equalsIgnoreCase( message.contentType ) ) {
                    multipart = new Attachments.HtmlMimeMultipart();
                    try {
                        Matcher m = Pattern.compile( "[\"']cid:(.+)[\"']" ).matcher( message.body );
                        while( m.find() )
                            cidIds.add( m.group( 1 ) );
                    } catch( Exception e ) {
                        log.warn( "Error scanning text/html body for cid-s", e );
                    }
                } else {
                    multipart = new MimeMultipart();
                }
                MimeBodyPart part = new MimeBodyPart();
                part.setContent( message.body, message.contentType + "; charset=UTF-8" );
                multipart.addBodyPart( part );
                for( Attachment attachment : message.attachments ) {
                    try {
                        part = new MimeBodyPart();
                        if( attachment.getFile() == null )
                            part.setContent( attachment.getContent(), Attachments.makeMimeType( attachment ) );
                        else if( attachment.getFile().startsWith( "classpath" ) )
                            part.setDataHandler( new DataHandler( new Attachments.ClasspathDataSource( attachment ) ) );
                        else if( attachment.getFile().startsWith( "http" ) )
                            part.setDataHandler( new DataHandler( new Attachments.MimeURLDataSource( attachment ) ) );
                        else part.setDataHandler( new DataHandler( new Attachments.MimeFileDataSource( attachment ) ) );
                        String cid = attachment.getContentId();
                        if( cid != null ) {
                            if( cidIds.contains( cid ) )
                                cid = "<" + cid + ">";
                            part.setHeader( "Content-ID", cid );
                        }
                        multipart.addBodyPart( part );
                    } catch( MalformedURLException e ) {
                        log.warn( "unable to attach file to email from URL {" + attachment.getFile() + "}", e );
                    }
                }
                mimeMessage.setContent( multipart );
            }
            Transport.send( mimeMessage );
            log.debug( "message {} is sent", message );
        } catch( MessagingException e ) {
            throw new MailException( e );
        }

    }

}
