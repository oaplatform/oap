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

package oap.ws.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Dictionary;
import oap.json.ext.Ext;
import oap.json.ext.ExtDeserializer;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.ws.Response;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.openapi.OpenapiIgnore;
import org.joda.time.DateTime;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import static ch.qos.logback.core.joran.util.beans.BeanUtil.getPropertyName;
import static ch.qos.logback.core.joran.util.beans.BeanUtil.isGetter;
import static java.util.Comparator.comparing;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.util.Strings.join;

@SuppressWarnings( "StringConcatenationInLoop" )
@Slf4j
@OpenapiIgnore
public class ApiWS {
    private final WebServices webServices;

    public ApiWS( WebServices webServices ) {
        this.webServices = webServices;
    }

    @WsMethod( produces = "text/plain", path = "/", method = GET, description = "Generates description of WS method with parameters and result" )
    public String api( Optional<Boolean> deprecated ) {
        boolean withDeprecated = deprecated.orElse( true );
        String result = "# SERVICES " + "#".repeat( 69 ) + "\n";
        Types types = new Types();
        Info info = new Info( webServices );
        for( Info.WebServiceInfo ws : info.services() ) {
            log.trace( "service {} -> {}", ws.context, ws.name );
            result += "## " + ws.name + " " + "#".repeat( Math.max( 0, 76 - ws.name.length() ) ) + "\n";
            result += "Bound to " + ws.context + "\n";
            result += "Methods:\n";

            for( Info.WebMethodInfo m : ws.methods( withDeprecated ) ) {
                log.trace( "method <{}>", m.name );
                List<String> parameters = new ArrayList<>();
                result += "\tMethod " + m.name
                    + ( m.deprecated ? " (Deprecated)" : "" ) + "\n";
                if( m.description.length() > 0 )
                    result += "\tDescription " + m.description + "\n";
                result += "\t" + m.methods + " /" + ws.context + m.path + "\n";
                result += "\tProduces " + m.produces + "\n";
                result += "\tRealm param " + m.realm + "\n";
                result += "\tPermissions " + m.permissions + "\n";
                result += "\tReturns " + formatType( m.resultType(), types ) + "\n";
                List<Info.WebMethodParameterInfo> params = m.parameters();
                if( params.isEmpty() ) result += "\tNo parameters\n";
                else {
                    result += "\tParameters\n";
                    for( Info.WebMethodParameterInfo p : params ) {
                        parameters.add( p.name );
                        result += "\t\t" + p.name + ": " + p.from + " " + formatType( p.type(), types ) + "\n";
                    }
                    log.trace( "parameters '{}'", parameters );
                }
                result += "\n";
            }
            result += "\n";
        }
        result += "# TYPES " + "#".repeat( 72 ) + "\n";
        for( Reflection type : types ) {
            if( ignorableType( type ) ) continue;
            result += "## " + type.name() + " " + "#".repeat( Math.max( 0, 76 - type.name().length() ) ) + "\n";
            result += formatComplexType( type, types, withDeprecated ) + "\n";
            result += "\n";
        }
        return result;
    }

    private boolean ignorableType( Reflection type ) {
        return type.assignableTo( Dictionary.class )
            || type.underlying.getSuperclass() == null
            || type.underlying == Reference.class;
    }

    private String formatType( Reflection r, Types types ) {
        if( r.isOptional() )
            return "optional " + formatType( r.typeParameters.get( 0 ), types );

        if( r.assignableFrom( SoftReference.class ) ) {
            if( !r.typeParameters.isEmpty() ) {
                return "soft reference -> " + formatType( r.typeParameters.get( 0 ), types );
            } else {
                return "soft reference -> <UNKNOWN>";
            }
        }

        if( r.assignableTo( Map.class ) ) return "map String -> " + formatType( r.getMapComponentsType()._2, types );
        if( r.assignableTo( Collection.class ) )
            return formatType( r.getCollectionComponentType(), types ) + "[]";
        if( r.assignableTo( Stream.class ) )
            return formatType( r.typeParameters.get( 0 ), types ) + "[]";
        if( r.assignableTo( Iterator.class ) )
            return formatType( r.typeParameters.get( 0 ), types ) + "[]";
        if( r.isArray() )
            return formatType( Reflect.reflect( r.underlying.componentType() ), types ) + "[]";
        if( r.isPrimitive() ) return r.underlying.getSimpleName();
        if( r.underlying.getPackageName().startsWith( DateTime.class.getPackageName() ) )
            return r.underlying.getSimpleName();
        if( r.underlying.equals( Date.class ) ) return r.underlying.getSimpleName();
        if( r.assignableTo( Integer.class ) ) return int.class.getSimpleName();
        if( r.assignableTo( Long.class ) ) return long.class.getSimpleName();
        if( r.assignableTo( Double.class ) ) return double.class.getSimpleName();
        if( r.assignableTo( Float.class ) ) return float.class.getSimpleName();
        if( r.assignableTo( Byte.class ) ) return byte.class.getSimpleName();
        if( r.assignableTo( Character.class ) ) return char.class.getSimpleName();
        if( r.assignableTo( String.class ) ) return String.class.getSimpleName();
        if( r.assignableTo( Boolean.class ) ) return Boolean.class.getSimpleName();
        if( r.isEnum() ) return join( ",", List.of( r.underlying.getEnumConstants() ), "[", "]", "\"" );
        if( r.assignableTo( Response.class ) ) return "<http response>";
        types.push( r );
        return r.name();
    }

    private String formatComplexType( Reflection r, Types types, boolean withDeprecated ) {
        var result = r.underlying.getSimpleName() + "\n";
        log.trace( "complex type {}", r.name() );
        List<Reflection.Field> fields = new ArrayList<>( r.fields.values() );
        fields.sort( comparing( Reflection.Field::name ) );
        result += "{\n";
        for( Reflection.Field f : fields ) {
            if( ignorable( f ) ) continue;
            log.trace( "type field {}", f.name() );
            if( !f.isAnnotatedWith( Deprecated.class ) || withDeprecated )
                result += "\t" + f.name() + ": " + formatField( r, f, types )
                    + ( f.isAnnotatedWith( Deprecated.class ) ? " (Deprecated)" : "" ) + "\n";
        }
        List<Reflection.Method> methods = r.methods;
        methods.sort( comparing( Reflection.Method::name ) );
        for( Reflection.Method m : methods ) {
            if( !ignorable( m, fields ) ) continue;
            log.trace( "type getter {}", m.name() );
            if( !m.isAnnotatedWith( Deprecated.class ) || withDeprecated )
                result += "\t" + getPropertyName( m.underlying ) + ": " + formatType( m.returnType(), types ) + "\n";
        }
        result += "\t".repeat( 0 ) + "}";
        return result;
    }

    private static boolean ignorable( Reflection.Field field ) {
        return field.isStatic()
            || field.underlying.isSynthetic()
            || field.findAnnotation( JsonIgnore.class ).isPresent();
    }

    private boolean ignorable( Reflection.Method m, List<Reflection.Field> fields ) {
        return !m.underlying.getDeclaringClass().equals( Object.class )
            && isGetter( m.underlying )
            && !m.isAnnotatedWith( JsonIgnore.class )
            && !Lists.map( fields, Reflection.Field::name ).contains( getPropertyName( m.underlying ) );
    }

    private String formatField( Reflection r, Reflection.Field f, Types types ) {
        Class<?> ext = f.type().assignableTo( Ext.class )
            ? ExtDeserializer.extensionOf( r.underlying, f.name() )
            : null;
        Reflection target = ext != null ? Reflect.reflect( ext ) : f.type();
        return formatType( target, types );
    }

    private static class Types implements Iterable<Reflection> {
        private final Set<Reflection> processed = new HashSet<>();
        private final Queue<Reflection> types = new LinkedList<>();

        public void push( Reflection type ) {
            if( !processed.contains( type ) && !types.contains( type ) ) types.add( type );
        }

        public Iterator<Reflection> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return !types.isEmpty();
                }

                @Override
                public Reflection next() {
                    Reflection next = types.poll();
                    processed.add( next );
                    return next;
                }
            };
        }
    }

}
