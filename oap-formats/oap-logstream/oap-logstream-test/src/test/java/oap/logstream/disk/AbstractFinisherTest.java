package oap.logstream.disk;

import oap.logstream.Timestamp;
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
        int safeInterval = 10;
        Timestamp timestamp = Timestamp.BPH_6;

        Path logs = testDirectoryFixture.testPath( "logs" );
        Files.createDirectory( logs );
        MockFinisher finisher = new MockFinisher( logs, safeInterval, List.of( "*.txt" ), timestamp );
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

        Files.setLastModifiedTime( file11, FileTime.fromMillis( 123453 ) );
        Files.setLastModifiedTime( file12, FileTime.fromMillis( 123454 ) );
        Files.setLastModifiedTime( file21, FileTime.fromMillis( 123455 ) );
        Files.setLastModifiedTime( file22, FileTime.fromMillis( 123456 ) );

        Dates.setTimeFixed( 123456 + Dates.m( 60 / timestamp.bucketsPerHour ) + safeInterval + 1 );


        finisher.run();

        assertThat( finisher.files )
            .hasSize( 4 );

        assertThat( finisher.files.subList( 0, 2 ) ).containsAnyOf(
            __( file12, new DateTime( 123454, UTC ).withMillisOfSecond( 0 ).withSecondOfMinute( 0 ) ),
            __( file22, new DateTime( 123456, UTC ).withMillisOfSecond( 0 ).withSecondOfMinute( 0 ) )
        );

        assertThat( finisher.files.subList( 2, 4 ) ).containsAnyOf(
            __( file11, new DateTime( 123453, UTC ).withMillisOfSecond( 0 ).withSecondOfMinute( 0 ) ),
            __( file21, new DateTime( 123455, UTC ).withMillisOfSecond( 0 ).withSecondOfMinute( 0 ) )
        );
    }
}
