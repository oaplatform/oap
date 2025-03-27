package oap.net;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IpRangeTreeTest {
    @Test
    public void testMinMax() {
        IpRangeTree<String> tree = new IpRangeTree<>( 8 );
        tree.addRange( IpUtils.ipv4AsLong( "0.0.0.0" ), IpUtils.ipv4AsLong( "255.255.255.255" ), "1" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "0.0.0.0" ) ) ).isEqualTo( "1" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "255.255.255.255" ) ) ).isEqualTo( "1" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "1.2.3.4" ) ) ).isEqualTo( "1" );
    }

    @Test
    public void testIntersection() {
        IpRangeTree<String> tree = new IpRangeTree<>( 8 );
        tree.addRange( IpUtils.ipv4AsLong( "0.0.0.0" ), IpUtils.ipv4AsLong( "200.255.255.255" ), "1" );
        tree.addRange( IpUtils.ipv4AsLong( "100.0.0.0" ), IpUtils.ipv4AsLong( "255.255.255.200" ), "2" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "0.0.0.0" ) ) ).isEqualTo( "1" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "255.255.255.200" ) ) ).isEqualTo( "2" );
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "120.100.100.100" ) ) ).isEqualTo( "2" );

        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "255.255.255.245" ) ) ).isNull();
    }

    @Test
    public void testIp() {
        IpRangeTree<String> tree = new IpRangeTree<>( 8 );
        tree.addRange( IpUtils.ipv4AsLong( "126.126.126.126" ), IpUtils.ipv4AsLong( "126.126.126.126" ), "1" );

        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "126.126.126.126" ) ) ).isEqualTo( "1" );

        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "126.126.126.125" ) ) ).isNull();
        assertThat( tree.lookUp( IpUtils.ipv4AsLong( "126.126.126.127" ) ) ).isNull();
    }
}
