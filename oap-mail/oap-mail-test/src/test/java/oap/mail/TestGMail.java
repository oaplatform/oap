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

import oap.io.Resources;
import oap.mail.test.MessageAssertion;
import oap.mail.test.MessagesAssertion;

public class TestGMail {

    /**
     * put gmailauth.conf in test/resources. Dont worry it's in .gitignore:
     * <p>
     * username=aaa@gmail.com
     * password=whatever
     */
    public static void main( String[] args ) throws MailException {
        Resources.filePath( TestGMail.class, "/gmailauth.conf" ).ifPresentOrElse( auth -> {
            PasswordAuthenticator authenticator = new PasswordAuthenticator( auth );
            SmtpTransport transport = new SmtpTransport( "smtp.gmail.com", 587, true, authenticator );
            Mailman mailman = new Mailman( transport, new MailQueue() );
            Template template = Template.of( "/xjapanese" ).orElseThrow();
            template.bind( "logo",
                "https://assets.coingecko.com/coins/images/4552/small/0xcert.png?1547039841" );
            Message message = template.buildMessage();
            message.from = MailAddress.of( "Україна", "vladimir.kirichenko@gmail.com" );
            message.to.add( MailAddress.of( "Little Green Mail", "vk@xenoss.io" ) );
            mailman.send( message );
            mailman.run();

            MessageAssertion.assertInboxMostRecentMessage( authenticator.username, authenticator.password )
                .hasSubject( "[japanese xtest] サーバの接続が切断されました" );

            MessagesAssertion.assertInbox( authenticator.username, authenticator.password )
                .bySubject( "[japanese xtest] サーバの接続が切断されました", MessageAssertion::assertMessage );
        }, () -> {
            throw new RuntimeException( "see javadoc!" );
        } );
    }
}
