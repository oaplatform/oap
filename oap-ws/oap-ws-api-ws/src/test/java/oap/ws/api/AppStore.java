package oap.ws.api;

import oap.dictionary.Dictionary;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public enum AppStore implements Dictionary {
    UNKNOWN( -1, false, "Unknown" ),
    GOOGLE_PLAY( 0, true, "Google Play" ),
    APP_STORE( 1, true, "Apple App Store" ),
    AMAZON( 2, false, "Amazon Android Store" ),
    WEB( 3, true, "Web page" );

    private final int externalId;
    private final boolean enabled;

    private final String title;

    public final String title() {
        return title;
    }

    AppStore( int externalId, boolean enabled, String title ) {
        this.externalId = externalId;
        this.enabled = enabled;
        this.title = title;
    }

    public static AppStore valueOf( int externalId ) {
        switch( externalId ) {
            case -1: return UNKNOWN;
            case 0: return GOOGLE_PLAY;
            case 1: return APP_STORE;
            case 2: return AMAZON;
            case 3: return WEB;
            default: return UNKNOWN;
        }
    }

    @Override
    public int getOrDefault( String id, int defaultValue ) {
        return defaultValue;
    }

    @Override
    public String getOrDefault( int externlId, String defaultValue ) {
        return defaultValue;
    }

    @Override
    public Integer get( String id ) {
        return null;
    }

    @Override
    public boolean containsValueWithId( String id ) {
        return false;
    }

    @Override
    public List<String> ids() {
        return emptyList();
    }

    @Override
    public int[] externalIds() {
        return new int[0];
    }

    @Override
    public Map<String, Object> getProperties() {
        return emptyMap();
    }

    @Override
    public Optional<? extends Dictionary> getValueOpt( String name ) {
        return Optional.empty();
    }

    @Override
    public Dictionary getValue( String name ) {
        return null;
    }

    @Override
    public Dictionary getValue( int externalId ) {
        return null;
    }

    @Override
    public List<? extends Dictionary> getValues() {
        return emptyList();
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public Optional<Object> getProperty( String name ) {
        return Optional.empty();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getExternalId() {
        return externalId;
    }
    @Override
    public AppStore cloneDictionary() {
        return this;
    }
    public int externalId() {
        return externalId;
    }

    @Override
    public boolean containsProperty( String name ) {
        return false;
    }
}
