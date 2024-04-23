package oap.application;

public enum ServiceEnabledStatus {
    ENABLED( "enabled" ),
    DISABLED_BY_FLAG( "'enabled' flag" ),
    DISABLED_BY_MODULE_FLAG( "'module.enabled' flag" );

    final String name;

    ServiceEnabledStatus( String name ) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
