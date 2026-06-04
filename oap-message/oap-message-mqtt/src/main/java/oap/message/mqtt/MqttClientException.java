package oap.message.mqtt;

public class MqttClientException extends RuntimeException {
    public MqttClientException() {
    }

    public MqttClientException( String message ) {
        super( message );
    }

    public MqttClientException( String message, Throwable cause ) {
        super( message, cause );
    }

    public MqttClientException( Throwable cause ) {
        super( cause );
    }

    public MqttClientException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
}
