package oap.logstream.formats.rowbinary;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.insert.InsertSettings;
import com.clickhouse.data.ClickHouseFormat;
import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.extern.slf4j.Slf4j;
import oap.template.Types;
import oap.testng.Fixtures;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

@Slf4j
public class RowBinaryTest extends Fixtures {
    public static final int HTTP_PORT = 8123;

    public static final String CLICKHOUSE_VERSION = "25.8.7.3-alpine";
    public static final String CLICKHOUSE_REPOASITORY = "clickhouse/clickhouse-server";
    private GenericContainer<?> container;

    public RowBinaryTest() {
    }

    @BeforeMethod
    @Override
    public void fixBeforeMethod() {
        super.fixBeforeMethod();

        Consumer<CreateContainerCmd> cmd = e -> {
            e.withName( "clickhouse-server-RowBinaryTest-testcontainers-" + RandomStringUtils.secure().nextAlphanumeric( 5 ) );
            e.withHostName( "clickhouse-server-RowBinaryTest-testcontainers" );
            e.withEnv(
                "CLICKHOUSE_DO_NOT_CHOWN=1",
                "CLICKHOUSE_SKIP_USER_SETUP=1",
                "AWS_EC2_METADATA_DISABLED=true"
            );
        };

        container = new GenericContainer<>( DockerImageName.parse( CLICKHOUSE_REPOASITORY + ":" + CLICKHOUSE_VERSION ) )
            .withCreateContainerCmdModifier( cmd )
            .withExposedPorts( HTTP_PORT )
            .withLogConsumer( new Slf4jLogConsumer( log ) );

        container.waitingFor(
            Wait
                .forHttp( "/" )
                .forPort( HTTP_PORT )
                .forStatusCode( 200 )
                .forResponsePredicate( "Ok."::equals )
                .withReadTimeout( Duration.ofSeconds( 5 ) )
                .withStartupTimeout( Duration.ofMinutes( 2 ) )
        );

        container.start();
    }

    @AfterMethod( alwaysRun = true )
    @Override
    public void fixAfterMethod() {
        container.stop();

        super.fixAfterMethod();
    }

    @Test
    public void testFormat() {
        try( Client client = new Client.Builder().addEndpoint( "http://localhost:" + container.getMappedPort( HTTP_PORT ) + "/" )
            .setUsername( "default" )
            .setPassword( "" )
            .build() ) {

            assertThat( client.execute( "CREATE TABLE TEST ( b Bool, bt UInt8, i Int32, l Int64, f Float32, d Float64, dt DateTime, date Date, ls Array(String) ) ENGINE=Memory" ) )
                .succeedsWithin( Duration.ofSeconds( 10 ) );

            assertThat( client.insert( "TEST", out -> {
                RowBinaryOutputStream rowBinaryOutputStream = new RowBinaryOutputStream( out, List.of( "b", "bt", "i", "l", "f", "d", "dt", "date", "ls" ), new byte[][] {
                    { Types.BOOLEAN.id }, { Types.BYTE.id }, { Types.INTEGER.id }, { Types.LONG.id }, { Types.FLOAT.id }, { Types.DOUBLE.id }, { Types.DATETIME.id }, { Types.DATE.id }, { Types.LIST.id, Types.STRING.id }
                } );
                rowBinaryOutputStream.writeBoolean( true );
                rowBinaryOutputStream.writeByte( ( byte ) 134 );
                rowBinaryOutputStream.writeInt( 12345 );
                rowBinaryOutputStream.writeLong( 1234567890123456789L );
                rowBinaryOutputStream.writeFloat( 123.45f );
                rowBinaryOutputStream.writeDouble( 123.4578901 );
                rowBinaryOutputStream.writeDateTime( new DateTime( 2025, 7, 10, 19, 21, 38, 123, UTC ) );
                rowBinaryOutputStream.writeDate( new Date( new DateTime( 2025, 7, 10, 19, 21, 38, 123, UTC ).getMillis() ) );
                rowBinaryOutputStream.writeList( List.of( "a", "b", "bb" ) );

                rowBinaryOutputStream.writeBoolean( false );
                rowBinaryOutputStream.writeByte( ( byte ) 1 );
                rowBinaryOutputStream.writeInt( 0 );
                rowBinaryOutputStream.writeLong( -123L );
                rowBinaryOutputStream.writeFloat( 0.045f );
                rowBinaryOutputStream.writeDouble( -10234567 );
                rowBinaryOutputStream.writeDateTime( new DateTime( 2025, 7, 10, 19, 21, 39, 124, UTC ) );
                rowBinaryOutputStream.writeDate( new Date( new DateTime( 2025, 7, 10, 19, 21, 39, 123, UTC ).getMillis() ) );
                rowBinaryOutputStream.writeList( List.of() );
            }, ClickHouseFormat.RowBinaryWithNamesAndTypes, new InsertSettings() ) )
                .succeedsWithin( Duration.ofSeconds( 10 ) );

            assertThat( client.query( "SELECT * FROM TEST FORMAT " + ClickHouseFormat.RowBinaryWithNamesAndTypes ) )
                .succeedsWithin( Duration.ofSeconds( 10 ) )
                .satisfies( resp -> {
                    RowBinaryInputStream rowBinaryInputStream = new RowBinaryInputStream( resp.getInputStream() );
                    assertThat( rowBinaryInputStream.headers ).isEqualTo( new String[] { "b", "bt", "i", "l", "f", "d", "dt", "date", "ls" } );
                    assertThat( rowBinaryInputStream.types ).isEqualTo( new byte[][] {
                        { Types.BOOLEAN.id }, { Types.BYTE.id }, { Types.INTEGER.id }, { Types.LONG.id }, { Types.FLOAT.id }, { Types.DOUBLE.id }, { Types.DATETIME.id }, { Types.DATE.id }, { Types.LIST.id, Types.STRING.id }
                    } );

                    assertThat( rowBinaryInputStream.readBoolean() ).isTrue();
                    assertThat( rowBinaryInputStream.readByte() ).isEqualTo( ( byte ) 134 );
                    assertThat( rowBinaryInputStream.readInt() ).isEqualTo( 12345 );
                    assertThat( rowBinaryInputStream.readLong() ).isEqualTo( 1234567890123456789L );
                    assertThat( rowBinaryInputStream.readFloat() ).isEqualTo( 123.45f );
                    assertThat( rowBinaryInputStream.readDouble() ).isEqualTo( 123.4578901 );
                    assertThat( rowBinaryInputStream.readDateTime() ).isEqualTo( new DateTime( 2025, 7, 10, 19, 21, 38, UTC ) );
                    assertThat( rowBinaryInputStream.readDate() ).isEqualTo( new Date( new DateTime( 2025, 7, 10, 0, 0, 0, UTC ).getMillis() ) );
                    assertThat( rowBinaryInputStream.readList( String.class ) ).isEqualTo( List.of( "a", "b", "bb" ) );

                    assertThat( rowBinaryInputStream.readBoolean() ).isFalse();
                    assertThat( rowBinaryInputStream.readByte() ).isEqualTo( ( byte ) 1 );
                    assertThat( rowBinaryInputStream.readInt() ).isEqualTo( 0 );
                    assertThat( rowBinaryInputStream.readLong() ).isEqualTo( -123L );
                    assertThat( rowBinaryInputStream.readFloat() ).isEqualTo( 0.045f );
                    assertThat( rowBinaryInputStream.readDouble() ).isEqualTo( -10234567 );
                    assertThat( rowBinaryInputStream.readDateTime() ).isEqualTo( new DateTime( 2025, 7, 10, 19, 21, 39, UTC ) );
                    assertThat( rowBinaryInputStream.readDate() ).isEqualTo( new Date( new DateTime( 2025, 7, 10, 0, 0, 0, UTC ).getMillis() ) );
                    assertThat( rowBinaryInputStream.readList( String.class ) ).isEqualTo( List.of() );
                } );

            assertThat( client.query( "SELECT * FROM TEST FORMAT " + ClickHouseFormat.CSVWithNames ) )
                .succeedsWithin( Duration.ofSeconds( 10 ) )
                .satisfies( resp -> {
                    BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( resp.getInputStream(), UTF_8 ) );
                    String csv = bufferedReader.lines().collect( Collectors.joining( "\n" ) );
                    assertThat( csv ).isEqualTo( """
                        "b","bt","i","l","f","d","dt","date","ls"
                        true,134,12345,1234567890123456789,123.45,123.4578901,"2025-07-10 19:21:38","2025-07-10","['a','b','bb']"
                        false,1,0,-123,0.045,-10234567,"2025-07-10 19:21:39","2025-07-10","[]\"""" );
                } );
        }
    }
}
