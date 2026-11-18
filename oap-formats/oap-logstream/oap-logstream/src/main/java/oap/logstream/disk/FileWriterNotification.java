package oap.logstream.disk;

import java.nio.file.Path;

public interface FileWriterNotification {
    /**
     * the file is closed and completed ({@code .metadata.completed}): ready for upload
     */
    void fileClosed( Path outFilename );

    /**
     * The sync of the file failed: the file takes no more data, it is closed without {@code .metadata.completed}
     * and the next buffer goes to the next file version. The file is repaired by {@link DiskLoggerBackend#recover()}
     * on the next start ({@link #fileClosed(Path)} is called then). With {@link SyncMode#PERIODIC} /
     * {@link SyncMode#ON_CLOSE} the data written since the last successful sync may be lost by an OS crash.
     */
    default void fileFailed( Path outFilename, Throwable cause ) {
    }
}
