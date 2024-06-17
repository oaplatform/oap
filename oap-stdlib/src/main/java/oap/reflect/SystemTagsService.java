package oap.reflect;

import lombok.extern.slf4j.Slf4j;
import oap.util.Strings;

import java.util.HashMap;
import java.util.Map;

/**
 * Class is able to collect system labels (which are any objects, like enums, strings, etc).
 * In order to process class it should be annotated with @SystemTagSupport.
 * Any field/method annotated with @SystemTag is processed. If system label gives a name, like
 *
 * @SystemTag( "Private property" ), then it will have such a category name, otherwise its name
 * will be assembled from field/method name. The result of processing is a map, where keys are
 * category names, and values are actual values of fields/methods.
 */
@Slf4j
public class SystemTagsService {

    public static final Object[] EMPTY_ARGS = {};

    private static void processNameAndValueFromField( Object object, Reflection.Field field, Map<String, Object> result ) {
        String key = field.findAnnotation( SystemTag.class ).orElseThrow().value();
        if( Strings.isEmpty( key ) ) key = field.name();
        try {
            Object value = field.get( object );
            result.put( key, value );
        } catch( Exception ex ) {
            log.warn( "Class '{}' has field '{}' annotated with @SystemTag, cannot get its value", object.getClass().getCanonicalName(), field.name(), ex );
        }
    }

    private static void processNameAndValueFromMethod( Object object, Reflection.Method method, Map<String, Object> result ) {
        String key = method.findAnnotation( SystemTag.class ).orElseThrow().value();
        if( Strings.isEmpty( key ) ) key = method.name();
        try {
            Object value = method.invoke( object, EMPTY_ARGS );
            result.put( key, value );
        } catch( Exception ex ) {
            log.warn( "Class '{}' has public method '{}' annotated with @SystemTag, it should not have any parameters, but it has {}", object.getClass().getCanonicalName(), method.name(), method.parameters, ex );
        }
    }

    public Map<String, Object> collectSystemTags( Object object ) {
        return process( object, ( obj, field, result ) -> processNameAndValueFromField( object, field, result ), ( obj, method, result ) -> processNameAndValueFromMethod( object, method, result ) );
    }

    private Map<String, Object> process( Object object, FieldProcessor fieldProcessor, MethodProcessor methodProcessor ) {
        //check if class fits
        if( object == null ) return Map.of();
        if( !object.getClass().isAnnotationPresent( SystemTagSupport.class ) ) {
            log.warn( "Class '{}' has not been annotated with @SystemTagSupport", object.getClass().getCanonicalName() );
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        //collect system labels, available in object
        Reflect.reflect( object.getClass() ).annotatedFields( SystemTag.class )
            .forEach( field -> {
                if( field.isStatic() ) {
                    log.warn( "Class '{}' has static field '{}' annotated with @SystemTag, it's not supported", object.getClass().getCanonicalName(), field.name() );
                    return;
                }
                fieldProcessor.process( object, field, result );
            } );
        Reflect.reflect( object.getClass() ).annotatedMethods( SystemTag.class )
            .forEach( method -> {
                if( !method.isPublic() ) {
                    log.warn( "Class '{}' has not 'public' method '{}' annotated with @SystemTag", object.getClass().getCanonicalName(), method.name() );
                    return;
                }
                if( method.isVoid() ) {
                    log.warn( "Class '{}' has 'public void' method '{}' annotated with @SystemTag, but it should return value.", object.getClass().getCanonicalName(), method.name() );
                    return;
                }
                methodProcessor.process( object, method, result );
            } );

        return result;
    }

    @FunctionalInterface
    public interface FieldProcessor {
        void process( Object object, Reflection.Field field, Map<String, Object> result );
    }

    @FunctionalInterface
    public interface MethodProcessor {
        void process( Object object, Reflection.Method method, Map<String, Object> result );
    }
}
