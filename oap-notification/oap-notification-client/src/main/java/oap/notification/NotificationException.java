package oap.notification;

/**
 * Thrown by a {@link NotificationTransport} when publishing or subscribing fails (e.g. a broker connection
 * or protocol error) — wraps the underlying transport-specific exception.
 */
public class NotificationException extends RuntimeException {
    public NotificationException() {
    }

    public NotificationException( String message ) {
        super( message );
    }

    public NotificationException( String message, Throwable cause ) {
        super( message, cause );
    }

    public NotificationException( Throwable cause ) {
        super( cause );
    }
}
