package oap.http;


import oap.net.Inet;

import java.net.InetAddress;
import java.util.Objects;

public enum Protocol {
    HTTP,
    HTTPS,
    LOCAL;

    public static boolean doesNotMatch( final String httpContextProtocol, final Protocol protocol ) {
        return !LOCAL.name().equals( httpContextProtocol ) &&
            !Objects.equals( httpContextProtocol, protocol.name() );
    }

    public static boolean isLocal( final InetAddress remoteAddress, final Protocol protocol ) {
        return LOCAL.equals( protocol ) && !Inet.isLocalAddress( remoteAddress );
    }
}
