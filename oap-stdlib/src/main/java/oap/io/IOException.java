package oap.io;

public class IOException extends RuntimeException {
    public IOException() {
    }

    public IOException( String message ) {
        super( message );
    }

    public IOException( String message, Throwable cause ) {
        super( message, cause );
    }

    public IOException( Throwable cause ) {
        super( cause );
    }
}
