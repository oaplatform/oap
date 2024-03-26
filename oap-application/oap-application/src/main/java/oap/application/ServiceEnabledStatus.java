package oap.application;

public enum ServiceEnabledStatus {
    ENABLED( "enabled" ),
    DISABLED_BY_PROFILE( "profile" );

    final String name;

    ServiceEnabledStatus( String name ) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
