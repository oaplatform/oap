package oap.ws.sso;

import java.util.Optional;

public interface RoleStorage {
    Optional<Roles> getRoles( String role );
}
