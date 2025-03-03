package oap.net;

public class IpUtils {
    private static final long CLASS_B_LOW = IpUtils.ipv4AsLong( "172.16.0.0" );
    private static final long CLASS_B_HIGH = IpUtils.ipv4AsLong( "172.31.255.255" );

    private static final long CARRIER_GRADE_LOW = IpUtils.ipv4AsLong( "100.64.0.0" );
    private static final long CARRIER_GRADE_HIGH = IpUtils.ipv4AsLong( "100.127.255.255" );

    /**
     * works only with IPv4 addresses
     *
     * @param ipAddressArg
     * @return
     */
    public static long ipv4AsLong( final String ipAddressArg ) {
        int d1 = ipAddressArg.indexOf( '.' );
        int d2 = ipAddressArg.indexOf( '.', d1 + 1 );
        int d3 = ipAddressArg.indexOf( '.', d2 + 1 );

        long value = Long.parseLong( ipAddressArg.substring( d3 + 1 ) );
        value |= Long.parseLong( ipAddressArg.substring( d2 + 1, d3 ) ) << 8;
        value |= Long.parseLong( ipAddressArg.substring( d1 + 1, d2 ) ) << 16;
        value |= Long.parseLong( ipAddressArg.substring( 0, d1 ) ) << 24;

        return value;
    }

    public static boolean isLocalAddress( String ipAddress, long ipv4 ) {
        if( "127.0.0.1".equals( ipAddress ) ) {
            return true; // single class A network
        }
        if( ipAddress.startsWith( "10." ) ) {
            return true; // single class A network
        }
        if( ipAddress.startsWith( "169.254." ) ) {
            return true; //  zero-configuration networking when Dynamic Host Configuration Protocol (DHCP) services are not available
        }
        if( ipAddress.startsWith( "192.168." ) ) {
            return true; // 256 contiguous class C networks
        }

        if( ipv4 >= CLASS_B_LOW && ipv4 <= CLASS_B_HIGH ) {
            return true; //16 contiguous class B networks
        }
        if( ipv4 >= CARRIER_GRADE_LOW && ipv4 <= CARRIER_GRADE_HIGH ) {
            return true; //Dedicated space for carrier-grade NAT deployment
        }
        return false;
    }
}
