package oap.logstream.disk;

import java.nio.file.Path;

public interface FileWriterNotification {
    void fileClosed( Path outFilename );
}
