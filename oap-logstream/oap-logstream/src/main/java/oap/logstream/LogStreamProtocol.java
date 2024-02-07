package oap.logstream;

import oap.message.MessageProtocol;

import java.util.stream.Stream;


@SuppressWarnings( "checkstyle:InterfaceIsType" )
public interface LogStreamProtocol {
    ProtocolVersion CURRENT_PROTOCOL_VERSION = ProtocolVersion.BINARY_V2;

    enum ProtocolVersion {
        TSV_V1( 1 ), BINARY_V2( 2 );
        public final int version;

        ProtocolVersion( int version ) {
            this.version = version;
        }

        public static ProtocolVersion valueOf( int protocolVersion ) {
            return Stream.of( values() ).filter( pv -> pv.version == protocolVersion ).findAny()
                .orElseThrow( () -> new IllegalArgumentException( "version " + protocolVersion ) );
        }
    }

    byte MESSAGE_TYPE = 20;
    short STATUS_BACKEND_LOGGER_NOT_AVAILABLE = 20000;
    short INVALID_VERSION = 20001;
    short STATUS_OK = MessageProtocol.STATUS_OK;
}
