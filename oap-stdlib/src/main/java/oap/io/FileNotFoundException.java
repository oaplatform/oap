package oap.io;

public class FileNotFoundException extends IOException {
    public FileNotFoundException( String message ) {
        super( message );
    }

    public FileNotFoundException( java.io.FileNotFoundException cause ) {
        super( cause );
    }
}
