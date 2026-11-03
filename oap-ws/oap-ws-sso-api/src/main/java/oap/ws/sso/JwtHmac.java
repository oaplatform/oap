package oap.ws.sso;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

final class JwtHmac {
    private JwtHmac() {}

    static byte[] key( String secret ) {
        byte[] key = secret.getBytes( UTF_8 );
        if( key.length == 0 ) throw new IllegalArgumentException( "JWT secret must not be empty" );
        // HMAC zero-pads keys to its 64-byte block size. Padding short legacy keys
        // to Nimbus's 32-byte minimum preserves their exact HS256 signatures.
        return key.length < 32 ? Arrays.copyOf( key, 32 ) : key;
    }
}
