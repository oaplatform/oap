package oap.ws.openapi;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is supposed to be used with WS class or method in order to mark them
 * as ignored for generation OpenAPI file.
 * When used on a bean field or getter, the property is left out of the generated schema.
 * Note: protected methods in any WS classes are also ignored
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD } )
public @interface OpenApiIgnore {
}
