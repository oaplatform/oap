package oap.ws.sso;

import com.nimbusds.jwt.JWTClaimsSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.ws.sso.WsSecurity.SYSTEM;

public class JwtToken {
    private final JWTClaimsSet decodedJWT;
    private final SecurityRoles roles;

    public JwtToken( JWTClaimsSet decodedJWT, SecurityRoles roles ) {
        this.decodedJWT = decodedJWT;
        this.roles = roles;
    }

    public String getUserId() {
        final Object id = decodedJWT.getClaim( "id" );
        return id instanceof String value ? value : null;
    }

    public String getUserEmail() {
        final Object user = decodedJWT.getClaim( "user" );
        return user instanceof String value ? value : null;
    }

    public String getOrganizationId() {
        final Object orgId = decodedJWT.getClaim( "org_id" );
        return orgId instanceof String value ? value : null;
    }

    public long getCounter() {
        final Object counter = decodedJWT.getClaim( "counter" );
        return counter instanceof Number value ? value.longValue() : 0;
    }

    public List<String> getPermissions( String organizationId ) {
        if( decodedJWT == null ) {
            return List.of();
        }

        final Object tokenRoles = decodedJWT.getClaim( "roles" );
        if( !( tokenRoles instanceof Map<?, ?> rolesByOrganization ) ) {
            return List.of();
        }
        final String role;
        if( rolesByOrganization.get( SYSTEM ) != null ) {
            role = ( String ) rolesByOrganization.get( SYSTEM );
        } else {
            role = ( String ) rolesByOrganization.get( organizationId );
        }
        if( role != null ) {
            return new ArrayList<>( roles.permissionsOf( role ) );
        }
        return List.of();
    }
}
