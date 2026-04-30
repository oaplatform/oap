package oap.http.client;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinSocketAddressResolver implements SocketAddressResolver {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void resolve( String host, int port, Map<String, Object> context, Promise<List<InetSocketAddress>> promise ) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName( host );

            int length = addresses.length;
            if( length > 0 ) {
                int i = length > 1 ? counter.incrementAndGet() % length : 0;
                promise.succeeded( List.of( new InetSocketAddress( addresses[i], port ) ) );
            } else {
                promise.failed( new UnknownHostException() );
            }

        } catch( Throwable ex ) {
            promise.failed( ex );
        }
    }
}
