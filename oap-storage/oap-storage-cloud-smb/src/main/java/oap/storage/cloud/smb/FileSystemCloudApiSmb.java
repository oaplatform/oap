package oap.storage.cloud.smb;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import lombok.extern.slf4j.Slf4j;
import oap.storage.cloud.BlobData;
import oap.storage.cloud.CloudException;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.FileSystem;
import oap.storage.cloud.FileSystemCloudApi;
import oap.storage.cloud.FileSystemConfiguration;
import oap.storage.cloud.ListOptions;
import oap.storage.cloud.PageSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * {@code fs.smb.container[.<configurationId>]} is {@code host[:port]/share} — one backend instance (and its
 * {@code CIFSContext} session) is created per configurationId, not per server.
 */
@Slf4j
public class FileSystemCloudApiSmb implements FileSystemCloudApi {
    private static final int DEFAULT_PORT = 445;

    private final String host;
    private final int port;
    private final String share;
    private final String basedir;
    private final CIFSContext cifsContext;

    public FileSystemCloudApiSmb( FileSystemConfiguration fileSystemConfiguration, String configurationId ) {
        String container = ( String ) fileSystemConfiguration.getOrThrow( "smb", configurationId, "container" );

        int slashIdx = container.indexOf( '/' );
        String hostPort = slashIdx >= 0 ? container.substring( 0, slashIdx ) : container;
        this.share = slashIdx >= 0 ? container.substring( slashIdx + 1 ) : "";
        if( this.share.isEmpty() ) {
            throw new CloudException( "fs.smb: container must include a share, e.g. host[:port]/share" );
        }

        int colonIdx = hostPort.lastIndexOf( ':' );
        if( colonIdx > 0 && colonIdx < hostPort.length() - 1
            && hostPort.substring( colonIdx + 1 ).chars().allMatch( Character::isDigit ) ) {
            this.host = hostPort.substring( 0, colonIdx );
            this.port = Integer.parseInt( hostPort.substring( colonIdx + 1 ) );
        } else {
            this.host = hostPort;
            this.port = DEFAULT_PORT;
        }

        Object identity = fileSystemConfiguration.get( "smb", configurationId, "identity" );
        String username = identity != null ? identity.toString() : "guest";

        Object credential = fileSystemConfiguration.get( "smb", configurationId, "credential" );
        String password = credential != null ? credential.toString() : "";

        Object domainObj = fileSystemConfiguration.get( "smb", configurationId, "domain" );
        String domain = domainObj != null ? domainObj.toString() : "";

        this.basedir = normalizeBasedir( fileSystemConfiguration.get( "smb", configurationId, "filesystem.basedir" ) );

        try {
            CIFSContext baseContext = new BaseContext( new PropertyConfiguration( new Properties() ) );
            this.cifsContext = baseContext.withCredentials( new NtlmPasswordAuthenticator( domain, username, password ) );
        } catch( CIFSException e ) {
            throw new CloudException( e );
        }
    }

    private static String normalizeBasedir( Object basedirObj ) {
        if( basedirObj == null ) return "";
        String str = basedirObj.toString();
        int start = 0, end = str.length();
        while( start < end && str.charAt( start ) == '/' ) start++;
        while( end > start && str.charAt( end - 1 ) == '/' ) end--;
        return str.substring( start, end );
    }

    private String physicalPath( String path ) {
        return basedir.isEmpty() ? path : basedir + "/" + path;
    }

    private String rawUrl( String physicalPath ) {
        return s( "smb://${host}:${port}/${share}/${physicalPath}" );
    }

    private String buildUrl( String path ) {
        return rawUrl( physicalPath( path ) );
    }

    private SmbFile smbFile( CloudURI path ) {
        try {
            return new SmbFile( buildUrl( path.path ), cifsContext );
        } catch( MalformedURLException e ) {
            throw new CloudException( e );
        }
    }

    private URI buildUri( CloudURI path ) {
        try {
            return new URI( "smb", null, host, port, "/" + share + "/" + physicalPath( path.path ), null, null );
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
    }

    private static String parentOf( String path ) {
        String normalized = path.endsWith( "/" ) ? path.substring( 0, path.length() - 1 ) : path;
        int idx = normalized.lastIndexOf( '/' );
        return idx >= 0 ? normalized.substring( 0, idx ) : "";
    }

    private void ensureParentDirectory( CloudURI path ) {
        String parent = physicalPath( parentOf( path.path ) );
        if( parent.isEmpty() ) return;

        try {
            StringBuilder current = new StringBuilder();
            for( String segment : parent.split( "/" ) ) {
                if( segment.isEmpty() ) continue;
                current.append( segment ).append( '/' );

                SmbFile dir = new SmbFile( rawUrl( current.toString() ), cifsContext );
                if( dir.exists() ) continue;

                try {
                    dir.mkdir();
                } catch( SmbException e ) {
                    // some SMB servers (observed against a FUSE/9p-backed share) report a spurious
                    // failure for a directory-create that actually succeeded, sometimes visible only
                    // after a short delay -- only propagate if a freshly-queried stat still disagrees
                    if( !directoryAppeared( current.toString() ) ) throw e;
                }
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    private boolean directoryAppeared( String physicalPath ) throws IOException {
        for( int attempt = 0; attempt < 5; attempt++ ) {
            if( new SmbFile( rawUrl( physicalPath ), cifsContext ).exists() ) return true;

            try {
                Thread.sleep( 100 );
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean blobExists( CloudURI path ) {
        try {
            SmbFile file = smbFile( path );
            return file.exists() && !file.isDirectory();
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public boolean containerExists( CloudURI path ) {
        try {
            new SmbFile( buildUrl( "" ), cifsContext ).exists();
            return true;
        } catch( IOException e ) {
            return false;
        }
    }

    @Override
    public void deleteBlob( CloudURI path ) {
        try {
            smbFile( path ).delete();
        } catch( IOException e ) {
            throw new CloudException( e );
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
        try {
            SmbFile file = smbFile( path );
            if( !file.exists() ) return null;

            return toStorageItem( path, file );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    private FileSystem.StorageItemImpl toStorageItem( CloudURI path, SmbFile file ) throws IOException {
        boolean directory = file.isDirectory();

        return new FileSystem.StorageItemImpl(
            path.path,
            "",
            buildUri( path ),
            new DateTime( file.lastModified(), DateTimeZone.UTC ),
            directory ? 0L : file.length(),
            directory ? "application/x-directory" : "" );
    }

    @Override
    public void downloadFile( CloudURI source, Path destination ) {
        try {
            oap.io.Files.ensureFile( destination );
            try( InputStream in = smbFile( source ).getInputStream();
                 OutputStream out = Files.newOutputStream( destination ) ) {
                in.transferTo( out );
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void copy( CloudURI source, CloudURI destination ) {
        try {
            ensureParentDirectory( destination );

            try( InputStream in = smbFile( source ).getInputStream();
                 OutputStream out = smbFile( destination ).getOutputStream() ) {
                in.transferTo( out );
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public InputStream getInputStream( CloudURI path ) {
        try {
            return smbFile( path ).getInputStream();
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI path, Map<String, String> tags ) {
        try {
            ensureParentDirectory( path );

            return smbFile( path ).getOutputStream();
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void upload( CloudURI destination, BlobData blobData ) {
        try {
            ensureParentDirectory( destination );

            try( OutputStream out = smbFile( destination ).getOutputStream() ) {
                switch( blobData.content ) {
                    case InputStream inputStream -> inputStream.transferTo( out );
                    case String str -> out.write( str.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
                    case byte[] bytes -> out.write( bytes );
                    case ByteBuffer byteBuffer -> out.write( byteBuffer.array() );
                    case File file -> {
                        try( InputStream fis = new FileInputStream( file ) ) {
                            fis.transferTo( out );
                        }
                    }
                    case Path path -> {
                        try( InputStream fis = Files.newInputStream( path ) ) {
                            fis.transferTo( out );
                        }
                    }
                    case null -> throw new CloudException( "content must not be null" );
                    default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
                }
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) {
        try {
            String normalizedDir = path.path.isEmpty() || path.path.endsWith( "/" ) ? path.path : path.path + "/";

            List<FileSystem.StorageItemImpl> all = new ArrayList<>();
            SmbFile dir = new SmbFile( buildUrl( normalizedDir ), cifsContext );
            walk( path, dir, normalizedDir, all );
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

            return new PageSet<>( nextToken, result );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    private void walk( CloudURI base, SmbFile dir, String dirPath, List<FileSystem.StorageItemImpl> acc ) throws IOException {
        if( !dir.exists() ) return;

        for( SmbFile child : dir.listFiles() ) {
            String name = child.getName();
            boolean directory = name.endsWith( "/" );
            String childName = directory ? name.substring( 0, name.length() - 1 ) : name;
            String childPath = dirPath + childName;

            if( directory ) {
                walk( base, child, childPath + "/", acc );
            } else {
                acc.add( toStorageItem( base.withPath( childPath ), child ) );
            }
        }
    }

    @Override
    public void close() {
        try {
            cifsContext.close();
        } catch( CIFSException e ) {
            log.debug( "error closing cifs context", e );
        }
    }
}
