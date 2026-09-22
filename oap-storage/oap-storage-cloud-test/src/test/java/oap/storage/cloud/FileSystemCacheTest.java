package oap.storage.cloud;

import oap.io.Files;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemCacheTest extends Fixtures {
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture() );
    }

    private final Path fsBasedir;
    private final Path cacheBasedir;

    public FileSystemCacheTest() {
        fsBasedir = testDirectoryFixture.testPath( "fs" );
        cacheBasedir = testDirectoryFixture.testPath( "cache" );
    }

    @Test
    public void testCacheMissPopulatesCache() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "miss.txt" ), "content", ContentWriter.ofString() );

            assertThat( fileSystem.getInputStream( new CloudURI( "fs", "miss.txt" ) ) ).hasContent( "content" );

            assertFile( cacheBasedir.resolve( "fs/miss.txt" ) ).hasContent( "content" );
        }
    }

    @Test
    public void testCacheHitDoesNotTouchSource() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "hit.txt" ), "original", ContentWriter.ofString() );
            Files.setLastModifiedTime( fsBasedir.resolve( "hit.txt" ), 1000L );

            Files.write( cacheBasedir.resolve( "fs/hit.txt" ), "original", ContentWriter.ofString() );
            Files.setLastModifiedTime( cacheBasedir.resolve( "fs/hit.txt" ), 2000L );

            // source changes after the cache was written -- cache stays newer, so it must still be served
            Files.write( fsBasedir.resolve( "hit.txt" ), "changed-on-source", ContentWriter.ofString() );
            Files.setLastModifiedTime( fsBasedir.resolve( "hit.txt" ), 1500L );

            assertThat( fileSystem.getInputStream( new CloudURI( "fs", "hit.txt" ) ) ).hasContent( "original" );
        }
    }

    @Test
    public void testStaleCacheIsRefreshed() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( cacheBasedir.resolve( "fs/stale.txt" ), "old", ContentWriter.ofString() );
            Files.setLastModifiedTime( cacheBasedir.resolve( "fs/stale.txt" ), 1000L );

            Files.write( fsBasedir.resolve( "stale.txt" ), "new", ContentWriter.ofString() );
            Files.setLastModifiedTime( fsBasedir.resolve( "stale.txt" ), 2000L );

            assertThat( fileSystem.getInputStream( new CloudURI( "fs", "stale.txt" ) ) ).hasContent( "new" );

            assertFile( cacheBasedir.resolve( "fs/stale.txt" ) ).hasContent( "new" );
        }
    }

    @Test
    public void testDownloadFileIsCacheAware() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "download.txt" ), "content", ContentWriter.ofString() );

            Path destination = testDirectoryFixture.testPath( "downloaded.txt" );
            fileSystem.downloadFile( new CloudURI( "fs", "download.txt" ), destination );

            assertFile( destination ).hasContent( "content" );
            assertFile( cacheBasedir.resolve( "fs/download.txt" ) ).hasContent( "content" );
        }
    }

    @Test
    public void testCopyIsCacheAware() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "copy-src.txt" ), "content", ContentWriter.ofString() );

            fileSystem.copy( new CloudURI( "fs", "copy-src.txt" ), new CloudURI( "fs", "copy-dest.txt" ), Map.of() );

            assertFile( cacheBasedir.resolve( "fs/copy-src.txt" ) ).hasContent( "content" );
            assertThat( fileSystem.getInputStream( new CloudURI( "fs", "copy-dest.txt" ) ) ).hasContent( "content" );
        }
    }

    @Test
    public void testDeletePropagatesToCache() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "delete.txt" ), "content", ContentWriter.ofString() );

            // populate the cache
            fileSystem.getInputStream( new CloudURI( "fs", "delete.txt" ) );
            assertFile( cacheBasedir.resolve( "fs/delete.txt" ) ).exists();

            fileSystem.deleteBlob( new CloudURI( "fs", "delete.txt" ) );

            assertFile( fsBasedir.resolve( "delete.txt" ) ).doesNotExist();
            assertFile( cacheBasedir.resolve( "fs/delete.txt" ) ).doesNotExist();
        }
    }

    @Test
    public void testDeleteWithoutCachedCopyDoesNotThrow() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( fsBasedir.resolve( "delete-no-cache.txt" ), "content", ContentWriter.ofString() );

            fileSystem.deleteBlob( new CloudURI( "fs", "delete-no-cache.txt" ) );

            assertFile( fsBasedir.resolve( "delete-no-cache.txt" ) ).doesNotExist();
            assertFile( cacheBasedir.resolve( "fs/delete-no-cache.txt" ) ).doesNotExist();
        }
    }

    @Test
    public void testNoCacheConfiguredBehavesAsWithoutCaching() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( "fs.file.filesystem.basedir", fsBasedir );

        try( FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( map ) ) ) {
            Files.write( fsBasedir.resolve( "no-cache.txt" ), "content", ContentWriter.ofString() );

            assertThat( fileSystem.getInputStream( new CloudURI( "file", "no-cache.txt" ) ) ).hasContent( "content" );
            assertFile( cacheBasedir.resolve( "no-cache.txt" ) ).doesNotExist();
        }
    }

    @Test
    public void testSingleCacheBacksMultipleSourceConfigurationIds() {
        Path fs2Basedir = testDirectoryFixture.testPath( "fs2" );

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( "fs.file.container.fs", "" );
        map.put( "fs.file.filesystem.basedir.fs", fsBasedir.toString() );
        map.put( "fs.file.container.fs2", "" );
        map.put( "fs.file.filesystem.basedir.fs2", fs2Basedir.toString() );
        map.put( "fs.file.container.cachefs", "" );
        map.put( "fs.file.filesystem.basedir.cachefs", cacheBasedir.toString() );
        map.put( "fs.file.cache.get.cachefs", "fs,fs2" );

        try( FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( map ) ) ) {
            Files.write( fsBasedir.resolve( "multi.txt" ), "from-fs", ContentWriter.ofString() );
            Files.write( fs2Basedir.resolve( "multi.txt" ), "from-fs2", ContentWriter.ofString() );

            assertThat( fileSystem.getInputStream( new CloudURI( "fs", "multi.txt" ) ) ).hasContent( "from-fs" );
            assertThat( fileSystem.getInputStream( new CloudURI( "fs2", "multi.txt" ) ) ).hasContent( "from-fs2" );

            assertFile( cacheBasedir.resolve( "fs/multi.txt" ) ).hasContent( "from-fs" );
            assertFile( cacheBasedir.resolve( "fs2/multi.txt" ) ).hasContent( "from-fs2" );
        }
    }

    private FileSystemConfiguration getFileSystemConfiguration() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( "fs.file.container.fs", "" );
        map.put( "fs.file.filesystem.basedir.fs", fsBasedir.toString() );

        map.put( "fs.file.container.cachefs", "" );
        map.put( "fs.file.filesystem.basedir.cachefs", cacheBasedir.toString() );
        map.put( "fs.file.cache.get.cachefs", "fs" );

        return new FileSystemConfiguration( map );
    }
}
