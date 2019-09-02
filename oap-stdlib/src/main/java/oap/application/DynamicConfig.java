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

package oap.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.json.Binder;
import oap.util.Hash;
import oap.util.Strings;
import org.apache.commons.lang3.event.EventListenerSupport;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DynamicConfig<T> {
    private final Class<T> implementation;
    private final URL updateLocation;
    final Control control;
    @JsonIgnore
    private EventListenerSupport<Listener> listeners = EventListenerSupport.create( Listener.class );
    @JsonIgnore
    private String md5;
    @JsonIgnore
    public T value;

    @JsonCreator
    public DynamicConfig( Class<T> implementation, URL defaultLocation ) {
        this( -1, implementation, defaultLocation, null );
    }

    @JsonCreator
    public DynamicConfig( long refreshInterval, Class<T> implementation, URL defaultLocation, URL updateLocation ) {
        this.implementation = implementation;
        this.updateLocation = updateLocation;
        loadConfiguration( defaultLocation );
        if( isUpdateable() ) try {
            loadConfiguration( updateLocation );
        } catch( Exception e ) {
            log.warn( "error updating configuration: " + e.getMessage() + " using default " + defaultLocation );
        }

        this.control = isUpdateable() ? new Control( refreshInterval ) : null;
    }

    public boolean isUpdateable() {
        return updateLocation != null;
    }

    private void loadConfiguration( URL location ) {
        String string = Strings.readString( location );
        this.value = Binder.hocon.unmarshal( implementation, string );
        this.md5 = Hash.md5( string );
    }

    public void addListener( Listener listener ) {
        this.listeners.addListener( listener );
    }

    public interface Listener {
        void configChanged();
    }

    class Control {
        private Scheduled scheduled;
        private long refreshInterval;

        public Control( long refreshInterval ) {
            this.refreshInterval = refreshInterval;
        }

        public void start() {
            if( refreshInterval > 0 )
                this.scheduled = Scheduler.scheduleWithFixedDelay( refreshInterval, TimeUnit.MILLISECONDS, this::sync );

        }

        public void stop() {
            Scheduled.cancel( scheduled );
        }

        void sync() {
            try {
                String oldmd5 = md5;
                loadConfiguration( updateLocation );
                if( !oldmd5.equals( md5 ) ) {
                    listeners.fire().configChanged();
                    log.debug( "configuration updated from " + updateLocation );
                } else log.debug( "remote config is not changed" );
            } catch( Exception e ) {
                log.warn( "error updating configuration: " + e.getMessage() );
            }
        }


    }
}
