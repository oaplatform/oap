package oap.notification;

/**
 * Delivery guarantee for a {@link Notification}, mirroring MQTT QoS levels. Only {@link NotificationTransport}
 * implementations backed by a protocol with delivery guarantees (e.g. MQTT) honor the distinction; others may
 * treat every level the same.
 */
public enum Qos {
    /** Delivered at most once — may be lost, never duplicated. */
    AT_MOST_ONCE,
    /** Delivered at least once — never lost, may be duplicated. */
    AT_LEAST_ONCE,
    /** Delivered exactly once — never lost, never duplicated. */
    EXACTLY_ONCE
}
