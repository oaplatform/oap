package oap.ws;

import oap.validation.ValidationMessage;

/**
 * Common part of the error response builders: the {@link ErrorResponseBuilder} they wrap and the message methods.
 * {@code B} is the concrete builder type, so the chained calls return it.
 */
public abstract class AbstractErrorResponseBuilder<B extends AbstractErrorResponseBuilder<B>> {
    protected final ErrorResponseBuilder builder;

    protected AbstractErrorResponseBuilder( ErrorResponseBuilder builder ) {
        this.builder = builder;
    }

    @SuppressWarnings( "unchecked" )
    protected B self() {
        return ( B ) this;
    }

    public B error( String error ) {
        builder.error( error );
        return self();
    }

    public B message( String message ) {
        builder.message( message );
        return self();
    }

    public B message( String code, String message ) {
        builder.message( code, message );
        return self();
    }

    public B message( ValidationMessage message ) {
        return message( message.code(), message.message() );
    }

    public abstract Response build();
}
