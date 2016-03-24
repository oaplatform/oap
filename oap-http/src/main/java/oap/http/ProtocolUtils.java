package oap.http;

import oap.net.Inet;

import java.net.InetAddress;
import java.util.Objects;

public final class ProtocolUtils {

    private ProtocolUtils() {
    }

    public static boolean isWrongProtocolConfigured( final String httpContextProtocol, final Protocol protocol ) {
        return !Protocol.LOCAL.name().equals( httpContextProtocol ) &&
            !Objects.equals( httpContextProtocol, protocol.name() );
    }

    public static boolean isLocal( final InetAddress remoteAddress, final Protocol protocol ) {
        return Protocol.LOCAL.equals( protocol ) && !Inet.isLocalAddress( remoteAddress );
    }
}
