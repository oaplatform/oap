package oap.io;

import java.io.File;

public class FileExistsException extends IOException {
    public FileExistsException( File file ) {
        super( "File " + file + " exists" );
    }

    public FileExistsException( IOException cause ) {
        super( cause );
    }
}
