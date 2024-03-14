package oap.testng;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.atomic.FileAtomicLong;
import oap.io.Sockets;

import java.io.UncheckedIOException;

@Slf4j
public class Ports {
    public static final FileAtomicLong LAST_PORT = new FileAtomicLong( "/tmp/port.lock", 1, 10000 );

    public static final int MIN_PORT_RANGE = 10000;
    public static final int MAX_PORT_RANGE = 30000;

    public static int getFreePort( Class<?> logClass ) throws UncheckedIOException {
        synchronized( LAST_PORT ) {
            int port;
            do {
                port = ( int ) LAST_PORT.updateAndGet( previousPort ->
                    previousPort > MAX_PORT_RANGE ? MIN_PORT_RANGE : previousPort + 1 );
            } while( !Sockets.isTcpPortAvailable( port ) );

            log.debug( "[{}] finding port... port={}", logClass, port );
            return port;
        }
    }
}
