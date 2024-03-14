package oap.concurrent.atomic;

import oap.io.Closeables;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileAtomicLongTest extends Fixtures {

    private final TestDirectoryFixture testDirectoryFixture;
    private FileAtomicLong fileAtomicLong1;
    private FileAtomicLong fileAtomicLong2;

    public FileAtomicLongTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        Path file = testDirectoryFixture.testPath( "al" );

        fileAtomicLong1 = new FileAtomicLong( file, 1, 0 );
        fileAtomicLong2 = new FileAtomicLong( file, 1, 0 );
    }

    @AfterMethod
    public void afterMethod() {
        Closeables.close( fileAtomicLong2 );
        Closeables.close( fileAtomicLong1 );
    }

    @Test
    public void testGetSet() {
        assertThat( fileAtomicLong1.get() ).isEqualTo( 0L );
        assertThat( fileAtomicLong2.get() ).isEqualTo( 0L );

        fileAtomicLong1.set( 16 );

        assertThat( fileAtomicLong1.get() ).isEqualTo( 16L );
        assertThat( fileAtomicLong2.get() ).isEqualTo( 16L );

        fileAtomicLong2.set( 17 );

        assertThat( fileAtomicLong1.get() ).isEqualTo( 17L );
        assertThat( fileAtomicLong2.get() ).isEqualTo( 17L );
    }

    @Test
    public void testGetAndSet() throws IOException {
        assertThat( fileAtomicLong1.getAndSet( -12L ) ).isEqualTo( 0L );
        assertThat( fileAtomicLong2.getAndSet( 1L ) ).isEqualTo( -12L );
        assertThat( fileAtomicLong1.getAndSet( 6L ) ).isEqualTo( 1L );
    }

    @Test
    public void testCompareAndSet() throws IOException {
        assertThat( fileAtomicLong1.compareAndSet( -1, -12L ) ).isFalse();
        assertThat( fileAtomicLong2.get() ).isEqualTo( 0L );
        assertThat( fileAtomicLong1.compareAndSet( 0, -12L ) ).isTrue();
        assertThat( fileAtomicLong2.get() ).isEqualTo( -12L );
        assertThat( fileAtomicLong1.compareAndSet( -1, 6L ) ).isFalse();
    }

    @Test
    public void testGetAndAnd() throws IOException {
        assertThat( fileAtomicLong1.getAndAdd( -12 ) ).isEqualTo( 0L );
        assertThat( fileAtomicLong2.getAndAdd( 1 ) ).isEqualTo( -12L );
        assertThat( fileAtomicLong1.getAndAdd( 5L ) ).isEqualTo( -11L );
        assertThat( fileAtomicLong1.getAndAdd( 5L ) ).isEqualTo( -6L );
    }

    @Test
    public void testAddAndGet() throws IOException {
        assertThat( fileAtomicLong1.addAndGet( -12 ) ).isEqualTo( -12L );
        assertThat( fileAtomicLong2.addAndGet( 1 ) ).isEqualTo( -11L );
        assertThat( fileAtomicLong1.addAndGet( 5L ) ).isEqualTo( -6L );
        assertThat( fileAtomicLong1.addAndGet( 5L ) ).isEqualTo( -1L );
    }

    @Test
    public void testGetAndUpdate() throws IOException {
        assertThat( fileAtomicLong1.getAndUpdate( old -> 12L ) ).isEqualTo( 0L );
        assertThat( fileAtomicLong2.getAndUpdate( old -> old * 2 ) ).isEqualTo( 12L );
        assertThat( fileAtomicLong1.getAndUpdate( old -> old * 3 ) ).isEqualTo( 24L );
    }

    @Test
    public void testUpdateAndGet() throws IOException {
        assertThat( fileAtomicLong1.updateAndGet( old -> 12L ) ).isEqualTo( 12L );
        assertThat( fileAtomicLong2.updateAndGet( old -> old * 2 ) ).isEqualTo( 24L );
        assertThat( fileAtomicLong1.updateAndGet( old -> old * 3 ) ).isEqualTo( 72L );
    }
}
