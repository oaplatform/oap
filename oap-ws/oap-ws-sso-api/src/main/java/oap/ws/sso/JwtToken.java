package oap.ws.sso;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.ws.sso.WsSecurity.SYSTEM;

public class JwtToken {
    private final DecodedJWT decodedJWT;
    private final SecurityRoles roles;

    public JwtToken( DecodedJWT decodedJWT, SecurityRoles roles ) {
        this.decodedJWT = decodedJWT;
        this.roles = roles;
    }

    public String getUserEmail() {
        final Claim user = decodedJWT.getClaims().get( "user" );
        return user != null ? user.asString() : null;
    }

    public String getOrganizationId() {
        final Claim orgId = decodedJWT.getClaims().get( "org_id" );
        return orgId != null ? orgId.asString() : null;
    }

    public long getCounter() {
        final Claim counter = decodedJWT.getClaims().get( "counter" );
        return counter != null ? counter.asLong() : 0;
    }

    public List<String> getPermissions( String organizationId ) {
        if( decodedJWT == null ) {
            return List.of();
        }

        final Claim tokenRoles = decodedJWT.getClaims().get( "roles" );
        if( tokenRoles == null ) {
            return List.of();
        }
        Map<String, Object> rolesByOrganization = tokenRoles.asMap();
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
