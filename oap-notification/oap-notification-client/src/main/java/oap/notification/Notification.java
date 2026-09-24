package oap.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import oap.json.Binder;
import oap.reflect.TypeRef;

import java.io.Serial;
import java.io.Serializable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Envelope around an already-serialized message payload sent/received through a {@link NotificationTransport}.
 * {@link #message} holds the raw JSON bytes as produced by the sender (see {@link NotificationService}, which
 * marshals the caller's payload before wrapping it here) — {@code Notification} itself does no serialization
 * or type resolution; read it back via {@link #messageAs(TypeRef)} (caller supplies the expected type) or
 * {@link #stringMessage()}.
 */
public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = -1730908173571715179L;

    public final byte[] message;

    @JsonCreator
    public Notification( byte[] message ) {
        this.message = message;
    }

    /**
     * Copies `message` from an existing notification — used by subclasses (e.g. {@link NotificationPublish}).
     */
    public Notification( Notification notification ) {
        this( notification.message );
    }

    public <T> T messageAs( TypeRef<T> typeReference ) {
        return Binder.json.unmarshal( typeReference, message );
    }

    public String stringMessage() {
        return new String( message, UTF_8 );
    }
}
