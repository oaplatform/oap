package oap.storage.cloud.ftp;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.storage.cloud.BlobData;
import oap.storage.cloud.CloudException;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.FileSystem;
import oap.storage.cloud.FileSystemCloudApi;
import oap.storage.cloud.FileSystemConfiguration;
import oap.storage.cloud.ListOptions;
import oap.storage.cloud.PageSet;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractFileSystemCloudApiFtp implements FileSystemCloudApi {
    protected final String host;
    protected final int port;
    protected final String username;
    protected final String password;
    protected final boolean passiveMode;

    protected AbstractFileSystemCloudApiFtp( FileSystemConfiguration fileSystemConfiguration, String scheme, String container ) {
        Object hostObj = fileSystemConfiguration.get( scheme, container, "jclouds.host" );
        this.host = hostObj != null ? hostObj.toString() : container;

        Object portObj = fileSystemConfiguration.get( scheme, container, "jclouds.port" );
        this.port = portObj != null ? Integer.parseInt( portObj.toString() ) : 21;

        Object identity = fileSystemConfiguration.get( scheme, container, "jclouds.identity" );
        this.username = identity != null ? identity.toString() : "anonymous";

        Object credential = fileSystemConfiguration.get( scheme, container, "jclouds.credential" );
        this.password = credential != null ? credential.toString() : "";

        Object passive = fileSystemConfiguration.get( scheme, container, "jclouds.passive-mode" );
        this.passiveMode = passive == null || Boolean.parseBoolean( passive.toString() );
    }

    protected abstract FTPClient createClient() throws IOException;

    protected void afterLogin( FTPClient client ) throws IOException {
    }

    protected FTPClient connect() {
        try {
            FTPClient client = createClient();
            client.connect( host, port );
            int reply = client.getReplyCode();
            if( !FTPReply.isPositiveCompletion( reply ) ) {
                client.disconnect();
                throw new CloudException( "FTP server refused connection " + host + ":" + port + " reply " + reply );
            }

            if( !client.login( username, password ) ) {
                client.disconnect();
                throw new CloudException( "FTP login failed for user " + username + " at " + host + ":" + port );
            }

            client.setFileType( FTP.BINARY_FILE_TYPE );
            if( passiveMode ) {
                client.enterLocalPassiveMode();
            } else {
                client.enterLocalActiveMode();
            }

            afterLogin( client );

            return client;
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    protected static void disconnect( FTPClient client ) {
        try {
            if( client.isConnected() ) {
                client.logout();
                client.disconnect();
            }
        } catch( IOException e ) {
            log.debug( "error disconnecting ftp client", e );
        }
    }

    private static String parentOf( String path ) {
        int idx = path.lastIndexOf( '/' );
        return idx >= 0 ? path.substring( 0, idx ) : "";
    }

    private static String nameOf( String path ) {
        int idx = path.lastIndexOf( '/' );
        return idx >= 0 ? path.substring( idx + 1 ) : path;
    }

    private void ensureRemoteDirectory( FTPClient client, String dirPath ) throws IOException {
        if( dirPath == null || dirPath.isEmpty() ) return;

        StringBuilder current = new StringBuilder();
        for( String segment : dirPath.split( "/" ) ) {
            if( segment.isEmpty() ) continue;
            current.append( '/' ).append( segment );
            if( !client.changeWorkingDirectory( current.toString() ) ) {
                client.makeDirectory( current.toString() );
            }
        }
    }

    private FTPFile findFile( FTPClient client, String path ) throws IOException {
        if( path.isEmpty() ) return null;

        FTPFile file = client.mlistFile( path );
        if( file != null ) return file;

        FTPFile[] files = client.listFiles( parentOf( path ) );
        if( files == null ) return null;

        String name = nameOf( path );
        for( FTPFile f : files ) {
            if( f.getName().equals( name ) ) return f;
        }
        return null;
    }

    private FileSystem.StorageItemImpl toStorageItem( CloudURI path, FTPFile file ) {
        DateTime lastModified = file.getTimestamp() != null
            ? new DateTime( file.getTimestamp().getTimeInMillis(), DateTimeZone.UTC )
            : null;

        return new FileSystem.StorageItemImpl(
            path.path,
            "",
            buildUri( path ),
            lastModified,
            file.getSize(),
            file.isDirectory() ? "application/x-directory" : "" );
    }

    private URI buildUri( CloudURI path ) {
        try {
            return new URI( path.scheme, null, host, port, "/" + path.path, null, null );
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public CompletableFuture<Boolean> blobExistsAsync( CloudURI path ) {
        FTPClient client = connect();
        try {
            return CompletableFuture.completedFuture( findFile( client, path.path ) != null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    @Override
    public CompletableFuture<Boolean> containerExistsAsync( CloudURI path ) {
        try {
            FTPClient client = connect();
            disconnect( client );
            return CompletableFuture.completedFuture( true );
        } catch( CloudException e ) {
            return CompletableFuture.completedFuture( false );
        }
    }

    @Override
    public CompletableFuture<Void> deleteBlobAsync( CloudURI path ) {
        FTPClient client = connect();
        try {
            if( !client.deleteFile( path.path ) ) {
                return CompletableFuture.failedFuture( new CloudException( "cannot delete " + path ) );
            }
            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    @Override
    public CompletableFuture<Void> deleteContainerAsync( CloudURI path ) throws CloudException {
        throw new CloudException( "not supported" );
    }

    @Override
    public CompletableFuture<Boolean> createContainerAsync( CloudURI path ) {
        return CompletableFuture.completedFuture( false );
    }

    @Override
    public CompletableFuture<Boolean> deleteContainerIfEmptyAsync( CloudURI path ) {
        return CompletableFuture.completedFuture( false );
    }

    @Override
    public CompletableFuture<? extends FileSystem.StorageItem> getMetadataAsync( CloudURI path ) {
        FTPClient client = connect();
        try {
            FTPFile file = findFile( client, path.path );
            if( file == null ) return CompletableFuture.completedFuture( null );

            return CompletableFuture.completedFuture( toStorageItem( path, file ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    @Override
    public CompletableFuture<Void> downloadFileAsync( CloudURI source, Path destination ) {
        FTPClient client = connect();
        try {
            oap.io.Files.ensureFile( destination );
            try( OutputStream out = Files.newOutputStream( destination ) ) {
                if( !client.retrieveFile( source.path, out ) ) {
                    return CompletableFuture.failedFuture( new CloudException( "cannot download " + source ) );
                }
            }
            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    @Override
    public CompletableFuture<Void> copyAsync( CloudURI source, CloudURI destination ) {
        Preconditions.checkArgument( source.scheme.equals( destination.scheme ) );

        FTPClient sourceClient = connect();
        FTPClient destinationClient = connect();
        try {
            InputStream in = sourceClient.retrieveFileStream( source.path );
            if( in == null ) {
                return CompletableFuture.failedFuture( new CloudException( "cannot open source stream " + source ) );
            }

            ensureRemoteDirectory( destinationClient, parentOf( destination.path ) );

            boolean stored = destinationClient.storeFile( destination.path, in );
            in.close();

            if( !stored || !sourceClient.completePendingCommand() ) {
                return CompletableFuture.failedFuture( new CloudException( "cannot copy " + source + " to " + destination ) );
            }

            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( sourceClient );
            disconnect( destinationClient );
        }
    }

    @Override
    public CompletableFuture<? extends InputStream> getInputStreamAsync( CloudURI path ) {
        FTPClient client = connect();
        try {
            InputStream in = client.retrieveFileStream( path.path );
            if( in == null ) {
                disconnect( client );
                return CompletableFuture.failedFuture( new CloudException( "cannot open " + path ) );
            }
            return CompletableFuture.completedFuture( new FtpInputStream( client, in ) );
        } catch( IOException e ) {
            disconnect( client );
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI path, Map<String, String> tags ) {
        FTPClient client = connect();
        try {
            ensureRemoteDirectory( client, parentOf( path.path ) );

            OutputStream out = client.storeFileStream( path.path );
            if( out == null ) {
                disconnect( client );
                throw new CloudException( "cannot open output stream for " + path );
            }
            return new FtpOutputStream( client, out );
        } catch( IOException e ) {
            disconnect( client );
            throw new CloudException( e );
        }
    }

    @Override
    public CompletableFuture<Void> uploadAsync( CloudURI destination, BlobData blobData ) {
        FTPClient client = connect();
        try {
            ensureRemoteDirectory( client, parentOf( destination.path ) );

            boolean stored = switch( blobData.content ) {
                case InputStream inputStream -> client.storeFile( destination.path, inputStream );
                case String str -> client.storeFile( destination.path, new ByteArrayInputStream( str.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) ) );
                case byte[] bytes -> client.storeFile( destination.path, new ByteArrayInputStream( bytes ) );
                case ByteBuffer byteBuffer -> client.storeFile( destination.path, new ByteArrayInputStream( byteBuffer.array() ) );
                case File file -> {
                    try( InputStream fis = new FileInputStream( file ) ) {
                        yield client.storeFile( destination.path, fis );
                    }
                }
                case Path path -> {
                    try( InputStream fis = Files.newInputStream( path ) ) {
                        yield client.storeFile( destination.path, fis );
                    }
                }
                case null -> throw new CloudException( "content must not be null" );
                default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
            };

            if( !stored ) {
                return CompletableFuture.failedFuture( new CloudException( "cannot upload to " + destination ) );
            }

            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    @Override
    public CompletableFuture<PageSet<? extends FileSystem.StorageItem>> listAsync( CloudURI path, ListOptions listOptions ) {
        FTPClient client = connect();
        try {
            List<FileSystem.StorageItemImpl> all = new ArrayList<>();
            walk( client, path, path.path, all );
            all.sort( Comparator.comparing( FileSystem.StorageItemImpl::getName ) );

            Stream<FileSystem.StorageItemImpl> stream = all.stream();
            int skip = listOptions.continuationToken != null ? Integer.parseInt( listOptions.continuationToken ) : 0;
            if( skip > 0 ) {
                stream = stream.skip( skip );
            }
            if( listOptions.maxKeys != null ) {
                stream = stream.limit( listOptions.maxKeys );
            }

            List<FileSystem.StorageItemImpl> result = stream.toList();

            String nextToken = listOptions.maxKeys != null ? String.valueOf( skip + result.size() ) : null;

            return CompletableFuture.completedFuture( new PageSet<>( nextToken, result ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        } finally {
            disconnect( client );
        }
    }

    private void walk( FTPClient client, CloudURI base, String dirPath, List<FileSystem.StorageItemImpl> acc ) throws IOException {
        FTPFile[] files = client.listFiles( dirPath );
        if( files == null ) return;

        String normalizedDir = dirPath.endsWith( "/" ) ? dirPath.substring( 0, dirPath.length() - 1 ) : dirPath;

        for( FTPFile file : files ) {
            if( ".".equals( file.getName() ) || "..".equals( file.getName() ) ) continue;

            String childPath = normalizedDir.isEmpty() ? file.getName() : normalizedDir + "/" + file.getName();

            if( file.isDirectory() ) {
                walk( client, base, childPath, acc );
            } else {
                acc.add( toStorageItem( base.withPath( childPath ), file ) );
            }
        }
    }

    @Override
    public void close() {
    }

    private static class FtpInputStream extends InputStream {
        private final FTPClient client;
        private final InputStream delegate;

        FtpInputStream( FTPClient client, InputStream delegate ) {
            this.client = client;
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            return delegate.read( b, off, len );
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            try {
                client.completePendingCommand();
            } finally {
                disconnect( client );
            }
        }
    }

    private static class FtpOutputStream extends OutputStream {
        private final FTPClient client;
        private final OutputStream delegate;

        FtpOutputStream( FTPClient client, OutputStream delegate ) {
            this.client = client;
            this.delegate = delegate;
        }

        @Override
        public void write( int b ) throws IOException {
            delegate.write( b );
        }

        @Override
        public void write( byte[] b, int off, int len ) throws IOException {
            delegate.write( b, off, len );
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
                if( !client.completePendingCommand() ) {
                    throw new CloudException( "incomplete ftp transfer" );
                }
            } finally {
                disconnect( client );
            }
        }
    }
}
