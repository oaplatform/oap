package oap.message.mqtt;

public interface MqttClientListener {
    default void onConnected( String name ) {}

    default void onDisconnected( String name ) {}
}
