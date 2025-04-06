package oap.logstream.disk;

import oap.logstream.Timestamp;
import oap.storage.cloud.FileSystemConfiguration;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class AbstractFinisherTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public AbstractFinisherTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void testSort() throws IOException {
        long safeInterval = 10;
        Timestamp timestamp = Timestamp.BPH_6;

        Path logs = testDirectoryFixture.testPath( "logs" );
        Files.createDirectory( logs );
        MockFinisher finisher = new MockFinisher( new FileSystemConfiguration( Map.of(
            "fs.default.clouds.scheme", "s3", "fs.default.jclouds.container", "test" ) ), logs, safeInterval, List.of( "*.txt" ), timestamp );
        finisher.start();

        finisher.priorityByType.put( "type2", 10 );

        Path file11 = Files.createFile( logs.resolve( "file1-type1.txt" ) );
        Path file12 = Files.createFile( logs.resolve( "file1-type2.txt" ) );
        Path file21 = Files.createFile( logs.resolve( "file2-type1.txt" ) );
        Path file22 = Files.createFile( logs.resolve( "file2-type2.txt" ) );

        LogMetadata type1 = new LogMetadata( "", "type1", "", Map.of(), new String[] {}, new byte[][] {} );
        LogMetadata type2 = new LogMetadata( "", "type2", "", Map.of(), new String[] {}, new byte[][] {} );

        type1.writeFor( file11 );
        type2.writeFor( file12 );
        type1.writeFor( file21 );
        type2.writeFor( file22 );

        long time = new DateTime( 2025, 4, 6, 14, 13, 39, 0, UTC ).getMillis();
        Files.setLastModifiedTime( file11, FileTime.fromMillis( time + 1 ) );
        Files.setLastModifiedTime( file12, FileTime.fromMillis( time + 2 ) );
        Files.setLastModifiedTime( file21, FileTime.fromMillis( time + 3 ) );
        Files.setLastModifiedTime( file22, FileTime.fromMillis( time + 4 ) );

        Dates.setTimeFixed( time + Dates.m( 60 / timestamp.bucketsPerHour ) + safeInterval + 10 );


        finisher.run();

        assertThat( finisher.files ).hasSize( 4 );

        assertThat( finisher.files.subList( 0, 2 ) ).containsAnyOf(
            __( file12, new DateTime( 2025, 4, 6, 14, 10, 0, 0, UTC ) ),
            __( file22, new DateTime( 2025, 4, 6, 14, 10, 0, 0, UTC ) )
        );

        assertThat( finisher.files.subList( 2, 4 ) ).containsAnyOf(
            __( file11, new DateTime( 2025, 4, 6, 14, 10, 0, 0, UTC ) ),
            __( file21, new DateTime( 2025, 4, 6, 14, 10, 0, 0, UTC ) )
        );
    }

    @Test
    public void testSafeInterval() throws IOException {
        long safeInterval = Dates.s( 30 );

        Timestamp timestamp = Timestamp.BPH_6;

        Path logs = testDirectoryFixture.testPath( "logs" );
        Files.createDirectory( logs );
        MockFinisher finisher = new MockFinisher( new FileSystemConfiguration( Map.of(
            "fs.default.clouds.scheme", "s3", "fs.default.jclouds.container", "test" ) ), logs, safeInterval, List.of( "*.txt" ), timestamp );
        finisher.start();

        Path file11 = Files.createFile( logs.resolve( "file1-type1-2025-04-05-15-02-10m.txt" ) );

        LogMetadata type1 = new LogMetadata( "", "type1", "", Map.of(), new String[] {}, new byte[][] {} );
        type1.writeFor( file11 );

        Files.setLastModifiedTime( file11, FileTime.fromMillis( new DateTime( 2025, 4, 5, 15, 20, 31, 0, UTC ).getMillis() ) );

        Dates.setTimeFixed( 2025, 4, 5, 15, 21, 0, 0 );

        System.out.println( timestamp.currentBucket( new DateTime( UTC ) ) );
        System.out.println( timestamp.toStartOfBucket( new DateTime( UTC ) ) );

        finisher.run();

        assertThat( finisher.files ).isEmpty();
    }
}
