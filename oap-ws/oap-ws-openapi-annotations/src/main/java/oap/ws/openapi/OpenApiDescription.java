package oap.ws.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is supposed to be used on a bean field or getter to provide
 * a description for the corresponding OpenAPI schema property.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD } )
public @interface OpenApiDescription {
    String value();
}
