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
import oap.storage.Storage;
import oap.util.Strings;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DefaultMailman implements Mailman, Runnable {
    private final String smtpHost;
    private final int smtpPort;
    private final Storage<Message> storage;
    protected String username;
    protected String password;
    private boolean startTls;
    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

    public DefaultMailman( String smtpHost, int smtpPort, boolean startTls, Storage<Message> storage ) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.startTls = startTls;
        this.storage = storage;
        initMailCap();

    }

    static String makeMimeType( Attachment attachment ) throws MessagingException {
        String name = attachment.getName();
        if( name == null ) {
            // try to construct the name from filename
            name = attachment.getFile();
            if( name != null ) {
                int p = name.lastIndexOf( '/' );
                if( p < 0 )
                    p = name.lastIndexOf( '\\' );
                if( p >= 0 )
                    name = name.substring( p + 1 );
            }
        }
        try {
            MimeType mt = new MimeType( attachment.getContentType() );
            if( name != null ) {
                String encoded = name;
                try {
                    encoded = new QCodec().encode( name, "UTF-8" );
                    String sub = encoded.substring( 10, encoded.length() - 2 );
                    if( sub.equals( name ) )
                        encoded = name;
                } catch( EncoderException e ) {
                    log.warn( "Encoging error for: " + name, e );
                }
                mt.setParameter( "name", encoded );
            }
            if( attachment.getFile() == null )
                mt.setParameter( "charset", "UTF-8" );
            return mt.toString();
        } catch( MimeTypeParseException e ) {
            throw new MessagingException( "Bad content type", e );
        }
    }

    public void start() {
        storage.forEach( messages::add );
    }

    private void initMailCap() {
        MailcapCommandMap mc = ( MailcapCommandMap ) CommandMap.getDefaultCommandMap();
        mc.addMailcap( "text/html;; x-java-content-handler=com.sun.mail.handlers.text_html" );
        mc.addMailcap( "text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml" );
        mc.addMailcap( "text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain" );
        mc.addMailcap( "multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed" );
        mc.addMailcap( "message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822" );
        CommandMap.setDefaultCommandMap( mc );
    }

    public void run() {
        Message message;

        var failed = new ArrayList<Message>();

        while( ( message = this.messages.poll() ) != null ) {
            try {
                sendNow( message );
                storage.delete( message.id );
            } catch( MailException e ) {
                log.error( e.toString(), e );
                failed.add( message );
            }
        }

        this.messages.addAll( failed );
    }

    @Override
    public void enqueue( Message message ) {
        messages.offer( message );
        storage.store( message );
    }

    public void sendNow( Message message ) throws MailException {
        Properties properties = new Properties();
        properties.put( "mail.smtp.host", smtpHost );
        properties.put( "mail.smtp.port", String.valueOf( smtpPort ) );
        properties.put( "mail.smtp.starttls.enable", String.valueOf( startTls ) );
        Authenticator authenticator = null;
        if( !Strings.isEmpty( username ) ) {
            properties.put( "mail.smtp.auth", "true" );
            authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication( username, password );
                }
            };
        }
        Session session = Session.getInstance( properties, authenticator );
        MimeMessage mimeMessage = new MimeMessage( session );
        try {
            mimeMessage.setFrom( message.getFrom().toInternetAddress() );
            for( MailAddress recipient : message.getTo() )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.TO, recipient.toInternetAddress() );
            for( MailAddress recipient : message.getCc() )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.CC, recipient.toInternetAddress() );
            for( MailAddress recipient : message.getBcc() )
                mimeMessage.addRecipient( javax.mail.Message.RecipientType.BCC, recipient.toInternetAddress() );
            mimeMessage.setSubject( Strings.toQuotedPrintable( message.getSubject() ) );
            mimeMessage.setHeader( "Content-Transfer-Encoding", "quoted-printable" );
            if( message.getAttachments().isEmpty() ) {
                mimeMessage.setContent( message.getBody(), message.getContentType() + "; charset=UTF-8" );
            } else {
                HashSet<String> cidIds = new HashSet<>();
                MimeMultipart multipart;
                if( "text/html".equalsIgnoreCase( message.getContentType() ) ) {
                    multipart = new HtmlMimeMultipart();
                    try {
                        Matcher m = Pattern.compile( "[\"']cid:(.+)[\"']" ).matcher( message.getBody() );
                        while( m.find() )
                            cidIds.add( m.group( 1 ) );
                    } catch( Exception e ) {
                        log.warn( "Error scanning text/html body for cid-s", e );
                    }
                } else {
                    multipart = new MimeMultipart();
                }
                MimeBodyPart part = new MimeBodyPart();
                part.setContent( message.getBody(), message.getContentType() + "; charset=UTF-8" );
                multipart.addBodyPart( part );
                for( Attachment attachment : message.getAttachments() ) {
                    part = new MimeBodyPart();
                    if( attachment.getFile() == null )
                        part.setContent( attachment.getContent(), makeMimeType( attachment ) );
                    else
                        part.setDataHandler( new DataHandler( new MimeFileDataSource( attachment ) ) );
                    String cid = attachment.getContentId();
                    if( cid != null ) {
                        if( cidIds.contains( cid ) )
                            cid = "<" + cid + ">";
                        part.setHeader( "Content-ID", cid );
                    }
                    multipart.addBodyPart( part );
                }
                mimeMessage.setContent( multipart );
            }
            Transport.send( mimeMessage );
            log.debug( "message sent to " + Arrays.asList( message.getTo() ) + ( message.getBcc().length > 0
                ? ", bcc to " + Arrays.asList( message.getBcc() ) : "" ) );
        } catch( MessagingException e ) {
            throw new MailException( e );
        }
    }

    @Slf4j
    static class HtmlMimeMultipart extends MimeMultipart {

        HtmlMimeMultipart() {
            try {
                this.setSubType( "related" );
            } catch( MessagingException e ) {
                log.warn( e.toString(), e );
            }
        }

        public String getContentType() {
            try {
                MimeType mt = new MimeType( super.getContentType() );
                mt.setParameter( "type", "text/html" );
                return mt.toString();
            } catch( MimeTypeParseException e ) {
                log.warn( e.toString(), e );
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
            if( mimeType != null )
                return mimeType;
            return super.getContentType();
        }
    }

}
