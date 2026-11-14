package oap.logstream;

import oap.message.MessageProtocol;

import java.util.stream.Stream;

import static dev.khbd.interp4j.core.Interpolations.s;


@SuppressWarnings( "checkstyle:InterfaceIsType" )
public interface LogStreamProtocol {
    ProtocolVersion CURRENT_PROTOCOL_VERSION = ProtocolVersion.ROW_BINARY_V4;
    byte MESSAGE_TYPE = 20;
    short STATUS_BACKEND_LOGGER_NOT_AVAILABLE = 20000;
    short INVALID_VERSION = 20001;
    short STATUS_OK = MessageProtocol.STATUS_OK;

    enum ProtocolVersion {
        TSV_V1( 1 ), BINARY_V2( 2 ),
        /**
         * header: digestion (long), data length (int), filePrefixPattern, logType, clientHostname, headers, types, properties.
         * payload - gzip
         */
        ROW_BINARY_V3( 3 ),
        /**
         * header: digestion (long), data length (int), compression ({@link oap.logstream.LogStreamCompression#id}, byte),
         * then as in v3. payload - compressed by the codec from the header
         */
        ROW_BINARY_V4( 4 );
        public final int version;

        ProtocolVersion( int version ) {
            this.version = version;
        }

        public static ProtocolVersion valueOf( int protocolVersion ) {
            return Stream.of( values() ).filter( pv -> pv.version == protocolVersion ).findAny()
                .orElseThrow( () -> new IllegalArgumentException( s( "version ${protocolVersion}" ) ) );
        }
    }

    enum ErrorStatus {
        ERROR_RETRY,
        ERROR_NO_RETRY
    }
}
