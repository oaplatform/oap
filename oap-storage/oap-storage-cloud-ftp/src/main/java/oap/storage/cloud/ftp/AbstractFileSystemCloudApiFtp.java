package oap.storage.cloud.ftp;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.storage.cloud.BlobData;
import oap.storage.cloud.CloudException;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.ContainerScopedCloudApi;
import oap.storage.cloud.FileSystem;
import oap.storage.cloud.FileSystemCloudApi;
import oap.storage.cloud.FileSystemConfiguration;
import oap.storage.cloud.ListOptions;
import oap.storage.cloud.PageSet;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public abstract class AbstractFileSystemCloudApiFtp implements FileSystemCloudApi, ContainerScopedCloudApi {
    private static final int DEFAULT_POOL_MAX_SIZE = 8;
    private static final long DEFAULT_POOL_MAX_WAIT_MILLIS = 30_000;

    protected final String host;
    protected final int port;
    protected final String username;
    protected final String password;
    protected final boolean passiveMode;
    protected final boolean removeEmptyFolders;

    private final GenericObjectPool<FTPClient> pool;

    protected AbstractFileSystemCloudApiFtp( FileSystemConfiguration fileSystemConfiguration, String scheme, String container ) {
        if( container == null || container.isBlank() ) {
            throw new CloudException( "fs." + scheme + ": container (ftp server host[:port]) is required" );
        }

        int colonIdx = container.lastIndexOf( ':' );
        if( colonIdx > 0 && colonIdx < container.length() - 1
            && container.substring( colonIdx + 1 ).chars().allMatch( Character::isDigit ) ) {
            this.host = container.substring( 0, colonIdx );
            this.port = Integer.parseInt( container.substring( colonIdx + 1 ) );
        } else {
            this.host = container;
            this.port = 21;
        }

        Object identity = fileSystemConfiguration.get( scheme, container, "jclouds.identity" );
        this.username = identity != null ? identity.toString() : "anonymous";

        Object credential = fileSystemConfiguration.get( scheme, container, "jclouds.credential" );
        this.password = credential != null ? credential.toString() : "";

        Object passive = fileSystemConfiguration.get( scheme, container, "jclouds.passive-mode" );
        this.passiveMode = passive == null || Boolean.parseBoolean( passive.toString() );

        Object removeEmptyFolders = fileSystemConfiguration.get( scheme, container, "jclouds.remove-empty-folders" );
        this.removeEmptyFolders = removeEmptyFolders != null && Boolean.parseBoolean( removeEmptyFolders.toString() );

        Object poolMaxSizeObj = fileSystemConfiguration.get( scheme, container, "jclouds.pool-max-size" );
        int poolMaxSize = poolMaxSizeObj != null ? Integer.parseInt( poolMaxSizeObj.toString() ) : DEFAULT_POOL_MAX_SIZE;

        Object poolMaxWaitObj = fileSystemConfiguration.get( scheme, container, "jclouds.pool-max-wait-millis" );
        long poolMaxWaitMillis = poolMaxWaitObj != null ? Long.parseLong( poolMaxWaitObj.toString() ) : DEFAULT_POOL_MAX_WAIT_MILLIS;

        GenericObjectPoolConfig<FTPClient> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal( poolMaxSize );
        poolConfig.setMaxWait( Duration.ofMillis( poolMaxWaitMillis ) );
        poolConfig.setBlockWhenExhausted( true );
        poolConfig.setTestOnBorrow( true );

        this.pool = new GenericObjectPool<>( new FtpClientPooledObjectFactory( this ), poolConfig );
    }

    protected abstract FTPClient createClient() throws IOException;

    protected void afterLogin( FTPClient client ) throws IOException {
    }

    FTPClient createAndLoginClient() {
        try {
            FTPClient client = createClient();
            client.connect( host, port );
            int reply = client.getReplyCode();
            if( !FTPReply.isPositiveCompletion( reply ) ) {
                client.disconnect();
                throw new CloudException( s( "FTP server refused connection ${host}:${port} reply ${reply}" ) );
            }

            if( !client.login( username, password ) ) {
                client.disconnect();
                throw new CloudException( s( "FTP login failed for user ${username} at ${host}:${port}" ) );
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

    private FTPClient borrow() {
        try {
            return pool.borrowObject();
        } catch( CloudException e ) {
            throw e;
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    private void release( FTPClient client, boolean healthy ) {
        try {
            if( healthy ) {
                pool.returnObject( client );
            } else {
                pool.invalidateObject( client );
            }
        } catch( Exception e ) {
            log.debug( "error releasing ftp client", e );
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

    /**
     * FTP commands are resolved relative to the client's current working directory, which
     * {@link #ensureRemoteDirectory} mutates as a side effect. Always addressing the server with
     * absolute paths keeps every operation independent of that CWD state.
     */
    private static String absolute( String path ) {
        return path.startsWith( "/" ) ? path : "/" + path;
    }

    private static String parentOf( String path ) {
        int idx = path.lastIndexOf( '/' );
        if( idx < 0 ) return "";
        if( idx == 0 ) return "/";
        return path.substring( 0, idx );
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
    public boolean blobExists( CloudURI path ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            boolean exists = findFile( client, absolute( path.path ) ) != null;
            healthy = true;
            return exists;
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
        }
    }

    @Override
    public boolean containerExists( CloudURI path ) {
        try {
            FTPClient client = borrow();
            release( client, true );
            return true;
        } catch( CloudException e ) {
            return false;
        }
    }

    @Override
    public void deleteBlob( CloudURI path ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            if( !client.deleteFile( absolute( path.path ) ) ) {
                healthy = true;
                throw new CloudException( "cannot delete " + path );
            }

            if( removeEmptyFolders ) {
                removeEmptyParents( client, parentOf( absolute( path.path ) ) );
            }

            healthy = true;
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
        }
    }

    private void removeEmptyParents( FTPClient client, String dirPath ) throws IOException {
        String parent = dirPath;
        while( !parent.isEmpty() && !"/".equals( parent ) ) {
            FTPFile[] children = client.listFiles( parent );
            boolean empty = children == null || children.length == 0
                || Arrays.stream( children )
                .allMatch( f -> ".".equals( f.getName() ) || "..".equals( f.getName() ) );

            if( !empty || !client.removeDirectory( parent ) ) break;

            parent = parentOf( parent );
        }
    }

    @Override
    public void deleteContainer( CloudURI path ) throws CloudException {
        throw new CloudException( "not supported" );
    }

    @Override
    public boolean createContainer( CloudURI path ) {
        return false;
    }

    @Override
    public boolean deleteContainerIfEmpty( CloudURI path ) {
        return false;
    }

    @Override
    public FileSystem.StorageItem getMetadata( CloudURI path ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            FTPFile file = findFile( client, absolute( path.path ) );
            healthy = true;
            if( file == null ) return null;

            return toStorageItem( path, file );
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
        }
    }

    @Override
    public void downloadFile( CloudURI source, Path destination ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            oap.io.Files.ensureFile( destination );
            try( OutputStream out = Files.newOutputStream( destination ) ) {
                if( !client.retrieveFile( absolute( source.path ), out ) ) {
                    throw new CloudException( "cannot download " + source );
                }
            }
            healthy = true;
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
        }
    }

    @Override
    public void copy( CloudURI source, CloudURI destination ) {
        Preconditions.checkArgument( source.scheme.equals( destination.scheme ) );

        FTPClient sourceClient = borrow();
        FTPClient destinationClient = borrow();
        boolean sourceHealthy = false;
        boolean destinationHealthy = false;
        try {
            InputStream in = sourceClient.retrieveFileStream( absolute( source.path ) );
            if( in == null ) {
                sourceHealthy = true;
                destinationHealthy = true;
                throw new CloudException( "cannot open source stream " + source );
            }

            ensureRemoteDirectory( destinationClient, parentOf( absolute( destination.path ) ) );

            boolean stored = destinationClient.storeFile( absolute( destination.path ), in );
            in.close();

            boolean completed = sourceClient.completePendingCommand();
            sourceHealthy = completed;
            destinationHealthy = stored;

            if( !stored || !completed ) {
                throw new CloudException( "cannot copy " + source + " to " + destination );
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( sourceClient, sourceHealthy );
            release( destinationClient, destinationHealthy );
        }
    }

    @Override
    public InputStream getInputStream( CloudURI path ) {
        FTPClient client = borrow();
        try {
            InputStream in = client.retrieveFileStream( absolute( path.path ) );
            if( in == null ) {
                release( client, true );
                throw new CloudException( "cannot open " + path );
            }
            return new FtpInputStream( this, client, in );
        } catch( IOException e ) {
            release( client, false );
            throw new CloudException( e );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI path, Map<String, String> tags ) {
        FTPClient client = borrow();
        try {
            ensureRemoteDirectory( client, parentOf( absolute( path.path ) ) );

            OutputStream out = client.storeFileStream( absolute( path.path ) );
            if( out == null ) {
                release( client, true );
                throw new CloudException( "cannot open output stream for " + path );
            }
            return new FtpOutputStream( this, client, out );
        } catch( IOException e ) {
            release( client, false );
            throw new CloudException( e );
        }
    }

    @Override
    public void upload( CloudURI destination, BlobData blobData ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            ensureRemoteDirectory( client, parentOf( absolute( destination.path ) ) );

            String remotePath = absolute( destination.path );
            boolean stored = switch( blobData.content ) {
                case InputStream inputStream -> client.storeFile( remotePath, inputStream );
                case String str -> client.storeFile( remotePath, new ByteArrayInputStream( str.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) ) );
                case byte[] bytes -> client.storeFile( remotePath, new ByteArrayInputStream( bytes ) );
                case ByteBuffer byteBuffer -> client.storeFile( remotePath, new ByteArrayInputStream( byteBuffer.array() ) );
                case File file -> {
                    try( InputStream fis = new FileInputStream( file ) ) {
                        yield client.storeFile( remotePath, fis );
                    }
                }
                case Path path -> {
                    try( InputStream fis = Files.newInputStream( path ) ) {
                        yield client.storeFile( remotePath, fis );
                    }
                }
                case null -> throw new CloudException( "content must not be null" );
                default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
            };

            if( !stored ) {
                throw new CloudException( "cannot upload to " + destination );
            }

            healthy = true;
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
        }
    }

    @Override
    public PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) {
        FTPClient client = borrow();
        boolean healthy = false;
        try {
            List<FileSystem.StorageItemImpl> all = new ArrayList<>();
            walk( client, path, absolute( path.path ), all );
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

            healthy = true;
            return new PageSet<>( nextToken, result );
        } catch( IOException e ) {
            throw new CloudException( e );
        } finally {
            release( client, healthy );
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
        pool.close();
    }

    private static class FtpInputStream extends InputStream {
        private final AbstractFileSystemCloudApiFtp owner;
        private final FTPClient client;
        private final InputStream delegate;

        FtpInputStream( AbstractFileSystemCloudApiFtp owner, FTPClient client, InputStream delegate ) {
            this.owner = owner;
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
            boolean healthy = false;
            try {
                delegate.close();
                healthy = client.completePendingCommand();
            } finally {
                owner.release( client, healthy );
            }
        }
    }

    private static class FtpOutputStream extends OutputStream {
        private final AbstractFileSystemCloudApiFtp owner;
        private final FTPClient client;
        private final OutputStream delegate;

        FtpOutputStream( AbstractFileSystemCloudApiFtp owner, FTPClient client, OutputStream delegate ) {
            this.owner = owner;
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
            boolean healthy = false;
            try {
                delegate.close();
                healthy = client.completePendingCommand();
                if( !healthy ) {
                    throw new CloudException( "incomplete ftp transfer" );
                }
            } finally {
                owner.release( client, healthy );
            }
        }
    }
}
