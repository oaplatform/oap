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

import flowctrl.integration.slack.SlackClientFactory;
import flowctrl.integration.slack.type.Payload;
import flowctrl.integration.slack.webhook.SlackWebhookClient;
import lombok.extern.slf4j.Slf4j;
import oap.alert.MessageTransport;

/**
 * Created by Admin on 02.06.2016.
 */
@Slf4j
public class SlackMessageTransport implements MessageTransport<Payload> {

    private final String webhookUrl;
    private SlackWebhookClient webhookClient;

    public SlackMessageTransport( String webhookUrl ) {
        this.webhookUrl = webhookUrl;
    }

    public synchronized void start() {
        webhookClient = SlackClientFactory.createWebhookClient( webhookUrl );
    }

    public synchronized void stop() {
        try {
            if( webhookClient != null ) {
                webhookClient.shutdown();
                webhookClient = null;
            }
        } catch( Throwable e ) {
            log.warn( e.getMessage(), e );
        }
    }

    public synchronized boolean isOperational() {
        return webhookClient != null;
    }

    public synchronized void ensureStarted() {
        if( !isOperational() ) {
            stop();
            start();
        }
    }

    @Override
    public void send( Payload p ) {
        ensureStarted();
        try {
            webhookClient.post( p );
        } catch( Exception e ) {
            throw new SlackCommunicationException( e );
        }
    }
}
