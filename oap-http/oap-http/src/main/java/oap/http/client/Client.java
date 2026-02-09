package oap.http.client;

import lombok.SneakyThrows;
import oap.util.Dates;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class Client {
    public static final HttpClient DEFAULT_HTTP_CLIENT = customHttpClient();
    public static final Executor DEFAULT_VIRTUAL_THREAD_EXECUTOR = ( ( QueuedThreadPool ) DEFAULT_HTTP_CLIENT.getExecutor() ).getVirtualThreadsExecutor();

    public static HttpClient customHttpClient() {
        return defaultInit( new HttpClient() );
    }

    public static HttpClient customHttpClient( HttpClientTransport transport ) {
        return defaultInit( new HttpClient( transport ) );
    }

    @SneakyThrows
    private static @Nonnull HttpClient defaultInit( HttpClient client ) {
        client.setConnectTimeout( Dates.s( 10 ) );
        QueuedThreadPool qtp = new QueuedThreadPool();
        qtp.setVirtualThreadsExecutor( new VirtualThreadPool() );
        client.setExecutor( qtp );
        client.setFollowRedirects( false );
        client.start();

        return client;
    }

    public static void resetCookies() {
        resetCookies( DEFAULT_HTTP_CLIENT );
    }

    public static void resetCookies( HttpClient httpClient ) {
        httpClient.getHttpCookieStore().clear();
    }
}
