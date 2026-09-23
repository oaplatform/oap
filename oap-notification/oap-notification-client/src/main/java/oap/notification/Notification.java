package oap.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import oap.json.TypeIdFactory;

import java.io.Serial;
import java.io.Serializable;

/**
 * Envelope around a user-defined, {@link Serializable} payload sent/received through a
 * {@link NotificationTransport}. {@link #message} is serialized to JSON with polymorphic type info
 * ({@code object:type}, via {@link TypeIdFactory}), so any concrete message class round-trips without the
 * caller needing to know the type up front — register message classes with {@code TypeIdFactory} the same
 * way {@code oap-statsdb} value classes are registered.
 */
public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = -1730908173571715179L;

    /** The domain payload — any {@link Serializable} type, deserialized polymorphically by its registered id. */
    @JsonTypeIdResolver( TypeIdFactory.class )
    @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "object:type" )
    public final Serializable message;

    @JsonCreator
    public Notification( Serializable message ) {
        this.message = message;
    }

    /** Copies `message` from an existing notification — used by subclasses (e.g. {@link NotificationPublish}). */
    public Notification( Notification notification ) {
        this( notification.message );
    }
}
