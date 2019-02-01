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

package oap.alert.slack;

import flowctrl.integration.slack.type.Attachment;
import flowctrl.integration.slack.type.Color;
import flowctrl.integration.slack.type.Payload;
import lombok.extern.slf4j.Slf4j;
import oap.alert.Alert;
import oap.alert.MessageStream;
import oap.alert.Messenger;

@Slf4j
public class SlackMessenger implements Messenger {
    private final String channel;
    private final String username;
    private final MessageStream<Payload> messageStream;

    public SlackMessenger( String channel, String username, MessageStream<Payload> messageStream ) {
        this.channel = channel;
        this.username = username;
        this.messageStream = messageStream;
    }

    @Override
    public void send( String host, String name, Alert alert, boolean changed ) {
        if( !changed )
            return;

        final Payload payload = generateMessage( host, name, alert );
        messageStream.send( payload );
    }

    private Payload generateMessage( String host, String name, Alert alert ) {
        final Payload payload = new Payload();
        payload.setText( "" );
        payload.setChannel( "#" + channel );
        payload.setUsername( username );

        final Attachment attachment = new Attachment();

        switch( alert.state ) {
            case GREEN:
                payload.setIcon_emoji( ":recycle:" );
                attachment.setColor( Color.GOOD );
                attachment.setText( "OK: " + name + "/" + host + ": " + alert.message );
                break;
            case YELLOW:
                payload.setIcon_emoji( ":warning:" );
                attachment.setColor( Color.WARNING );
                attachment.setText( "WARNING: " + name + "/" + host + ": " + alert.message );
                break;
            case RED:
                payload.setIcon_emoji( ":bangbang:" );
                attachment.setColor( Color.DANGER );
                attachment.setText( "CRITICAL: " + name + "/" + host + ": " + alert.message );
                break;
        }

        payload.addAttachment( attachment );
        return payload;
    }
}
