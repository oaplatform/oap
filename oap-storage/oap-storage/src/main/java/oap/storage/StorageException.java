package oap.storage;

import java.io.Serial;

public class StorageException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8806456838574024682L;

    public StorageException() {
    }

    public StorageException( String message ) {
        super( message );
    }
}
