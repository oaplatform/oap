/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.openapi.swagger;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.util.Pair;
import oap.util.Strings;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessorType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@ToString
public class DeprecationAnnotationResolver extends ModelResolver implements ModelConverter {
    private ModelConverterContext context;
    private final LinkedHashSet<String> toBeResolvedClassName = new LinkedHashSet<>();
    private final Map<Pair<String, String>, Schema> extensionsSchemas = new HashMap<>();
    private final Map<Pair<String, String>, Schema> collectionsSchemas = new HashMap<>();

    public DeprecationAnnotationResolver( ModelResolver modelResolver ) {
        super( modelResolver.objectMapper() );
    }

    @Override
    public Schema resolve( AnnotatedType annotatedType,
                           ModelConverterContext context,
                           Iterator<ModelConverter> next ) {
        this.context = context;
        String canonName = null;
        if ( annotatedType.getType().getClass() != SimpleType.class ) {
            this.toBeResolvedClassName.add( annotatedType.getType().getTypeName() );
        } else {
            canonName = ( ( SimpleType ) annotatedType.getType() ).getRawClass().getCanonicalName();
        }
        Schema resolved = super.resolve( annotatedType, context, next );
        if ( canonName != null
            && resolved != null
            && "object".equals( resolved.getType() )
            && !canonName.startsWith( "io.swagger." )
            && !"java.lang.Object".equals( canonName ) ) {
            resolved.setName( canonName.replace( '.', '_' ) );
        }
        return resolved;
    }

    @Override
    protected void applyBeanValidatorAnnotations( Schema property, Annotation[] annotations, Schema parent, boolean applyNotNullAnnotations ) {
        super.applyBeanValidatorAnnotations( property, annotations, parent, applyNotNullAnnotations );
        if( annotations == null || annotations.length == 0 ) return;
        Optional<Annotation> deprecated = Arrays.stream( annotations ).filter( anno -> anno.annotationType().equals( Deprecated.class ) ).findAny();
        deprecated.ifPresent( annotation -> {
            Deprecated anno = ( Deprecated ) annotation;
            property.setDeprecated( true );
            String since = !Strings.isEmpty( anno.since() ) ? " since: " + anno.since() : "";
            if( property.getName() != null )
                log.debug( "Field '{}' marked as deprecated{}", property.getName(), since );
        } );
    }

    /**
     * This method is called for every property in every bean, so we use that fact to catch up extension field like
     * 'private Ext ext' to identify which exact type is there.
     * @param member field in class to analyze
     * @param xmlAccessorTypeAnnotation
     * @param propName
     * @param propertiesToIgnore
     * @param propDef
     * @return true if the member needs to be ignored.
     */
    @Override
    protected boolean ignore( Annotated member,
                              XmlAccessorType xmlAccessorTypeAnnotation,
                              String propName,
                              Set<String> propertiesToIgnore,
                              BeanPropertyDefinition propDef ) {
        if( member.getClass() == AnnotatedField.class ) {
            //only for fields
            if( propDef.getPrimaryMember() != null && Ext.class.isAssignableFrom( member.getRawType() ) ) {
                Class clazz = propDef.getPrimaryMember().getDeclaringClass();
                String fieldName = propDef.getName();
                String memberClassName = clazz.getSimpleName();
                try {
                    log.debug( "Looking for '{}.{}' ...", clazz.getCanonicalName(), propDef.getName() );
                    Class<?> ext = ExtDeserializer.extensionOf( clazz, propDef.getName() );
                    if ( ext == null && !toBeResolvedClassName.isEmpty() ) {
                        //maybe a subclass of clazz? let's check up 'toBeResolved'
                        log.debug( "Looking for '{}.{}' ...", toBeResolvedClassName, propDef.getName() );
                        Class concreteClass = getConcreteClass();
                        ext = ExtDeserializer.extensionOf( concreteClass, propDef.getName() );
                        memberClassName = concreteClass.getSimpleName();
                    }
                    if ( ext != null ) {
                        Pair<String, String> key = Pair.__( memberClassName, fieldName );
                        AnnotatedType annotatedType = new AnnotatedType( ext );
                        Schema extensionSchema = super.resolve( annotatedType, context, context.getConverters() );
                        extensionsSchemas.put( key, extensionSchema );
                        log.debug( "Field '{}' in class '{}' has dynamic extension with class {}", fieldName, clazz.getCanonicalName(), ext.getCanonicalName() );
                    } else {
                        log.error( "Cannot resolve member '{}' in class '{}', ext is not defined", fieldName, clazz.getCanonicalName() );
                    }
                } catch( Exception ex ) {
                    log.error( "Cannot resolve member '{}' in class '{}', reason: {}", fieldName, clazz.getCanonicalName(), ex.getMessage() );
                }
            }
            if( propDef.getPrimaryMember() != null ) {
                String className = propDef.getPrimaryMember().getDeclaringClass().getSimpleName();
                String fieldName = propDef.getName();
                Class<?> rawType = member.getRawType();
                if( isArrayType( rawType ) ) {
                    AnnotatedType annotatedType = new AnnotatedType( rawType );
                    Schema schema = super.resolve( annotatedType, context, context.getConverters() );
                    Schema arraySchema = copySchemaFields( schema, propDef );
                    if( arraySchema != null ) collectionsSchemas.put( Pair.__( className, fieldName ), arraySchema );
                }
            }
        }
        return super.ignore( member, xmlAccessorTypeAnnotation, propName, propertiesToIgnore, propDef );
    }

    Class getConcreteClass() throws ReflectiveOperationException {
        Class result = null;
        List<String> classesToCheck = new ArrayList<>( toBeResolvedClassName );
        Collections.reverse( classesToCheck );
        for ( String name : classesToCheck ) {
            result = Class.forName( name );
            if ( !result.isInterface() && !Modifier.isAbstract( result.getModifiers() ) ) break;
        }
        return result;
    }

    Schema copySchemaFields( Schema schemaFrom, BeanPropertyDefinition propDef ) {
        if( schemaFrom == null ) return null;
        if( schemaFrom instanceof ArraySchema ) return schemaFrom;
        Schema schemaTo = new ArraySchema();
        schemaTo.setName( schemaFrom.getName() );
        schemaTo.set$id( schemaFrom.get$id() );
        if( schemaFrom.getItems() != null ) schemaTo.setItems( schemaFrom.getItems() );
        else {
            Type type = ( ( Field ) propDef.getPrimaryMember().getMember() ).getGenericType();
            //oap.util.Stream<java.lang.String>
            String innerType = type.getTypeName().replaceFirst( "[^<>]++<([^<>]++)>", "$1" );
            Schema typeSchema = detectInnerSchema( innerType );
            schemaTo.setItems( typeSchema );
        }
        schemaTo.setDeprecated( schemaFrom.getDeprecated() );

//        schemaTo.setProperties( schemaFrom.getProperties() );
//        schemaTo.setContentSchema( schemaFrom.getContentSchema() );
//        schemaTo.setAdditionalProperties( schemaFrom.getAdditionalProperties() );
//        schemaTo.set$schema( schemaFrom.get$schema() );
//        schemaTo.set$anchor( schemaFrom.get$anchor() );
//        schemaTo.set$ref( schemaFrom.get$ref() );
//        schemaTo.setAdditionalItems( schemaFrom.getAdditionalItems() );
//        schemaTo.set$comment( schemaFrom.get$comment() );
//        schemaTo.setAllOf( schemaFrom.getAllOf() );
//        schemaTo.setAnyOf( schemaFrom.getAnyOf() );
//        schemaTo.setBooleanSchemaValue( schemaFrom.getBooleanSchemaValue() );
//        schemaTo.setConst( schemaFrom.getConst() );
//        schemaTo.setContains( schemaFrom.getContains() );
//        schemaTo.setContentEncoding( schemaFrom.getContentEncoding() );
//        schemaTo.setContentMediaType( schemaFrom.getContentMediaType() );
//        schemaTo.setDefault( schemaFrom.getDefault() );
//        schemaTo.setDependentRequired( schemaFrom.getDependentRequired() );
//        schemaTo.setDependentSchemas( schemaFrom.getDependentSchemas() );
//        schemaTo.setDescription( schemaFrom.getDescription() );
//        schemaTo.setDiscriminator( schemaFrom.getDiscriminator() );
//        schemaTo.setExternalDocs( schemaFrom.getExternalDocs() );
//        schemaTo.setFormat( schemaFrom.getFormat() );
        return schemaTo;
    }

    @Nullable
    public static Schema detectInnerSchema( String innerType ) {
        Schema typeSchema = switch( innerType ) {
            case "java.lang.Integer" -> new IntegerSchema();
            case "java.lang.Long",
                 "java.lang.Double",
                 "java.lang.Float",
                 "java.lang.Byte" ->  new NumberSchema();
            case "java.lang.String" -> new StringSchema();
            case "byte[]" -> new ByteArraySchema();
            case "java.util.Date" -> new DateSchema();
            case "boolean" -> new BooleanSchema();
            default -> null;
        };
        if( typeSchema == null ) {
            log.debug( "Cannot inline schema for non-primitive type " + innerType );

        }
        return typeSchema;
    }

    public boolean isArrayType( Class rawType ) {
        return Stream.class.isAssignableFrom( rawType )
            || Iterator.class.isAssignableFrom( rawType );
    }

    public Schema getExtSchema( String className, String fieldName ) {
        return extensionsSchemas.get( Pair.__( className, fieldName ) );
    }

    public Schema getArraySchema( String className, String fieldName ) {
        return collectionsSchemas.get( Pair.__( className, fieldName ) );
    }
}
