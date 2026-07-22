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

package oap.ws.admin;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.application.ModuleItem;
import oap.json.Binder;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER;

@Slf4j
public class InspectorWS {
    private final JPathWS jPathWS;
    private final Kernel kernel;
    private final VelocityEngine engine = new VelocityEngine();

    public InspectorWS( JPathWS jPathWS, Kernel kernel ) {
        this.jPathWS = jPathWS;
        this.kernel = kernel;
        engine.setProperty( RESOURCE_LOADER, "classpath" );
        engine.setProperty( "classpath.resource.loader.class", ClasspathResourceLoader.class.getName() );
        engine.init();
    }

    @WsMethod( method = GET, path = "/ui", produces = "text/html" )
    public String ui() {
        List<String> services = jPathWS.listServices( "*" );
        VelocityContext context = new VelocityContext();
        context.put( "services", services );
        StringWriter writer = new StringWriter();
        engine.getTemplate( "oap/ws/admin/inspector-ui.html.vm" ).merge( context, writer );
        return writer.toString();
    }

    @WsMethod( method = GET, path = "/ui/{serviceReference}", produces = "text/html" )
    public String service( @WsParam( from = PATH ) String serviceReference ) {
        int dot = serviceReference.indexOf( '.' );
        ModuleItem.ServiceItem item = dot < 0 ? null
            : kernel.services.get( serviceReference.substring( 0, dot ), serviceReference.substring( dot + 1 ) );

        VelocityContext context = new VelocityContext();
        context.put( "found", item != null );
        context.put( "reference", serviceReference );
        if( item != null ) {
            context.put( "module", item.getModuleName() );
            context.put( "serviceName", item.serviceName );
            context.put( "implementation", item.service.implementation );
            context.put( "enabled", item.enabled.toString() );
            context.put( "dependsOn", item.dependsOn.stream()
                .map( d -> d.serviceItem.toString() + ( d.required ? "" : " (optional)" ) )
                .collect( Collectors.toList() ) );
            context.put( "supervision", item.service.supervision.toString() );
            context.put( "listen", item.service.listen );
            context.put( "link", item.service.link );
            context.put( "parameters", item.service.parameters );

            putInspectTables( context, item.instance, item.getReflection(),
                item.getModuleName() + "." + item.serviceName + ".instance" );
        }
        StringWriter writer = new StringWriter();
        engine.getTemplate( "oap/ws/admin/inspector-service.html.vm" ).merge( context, writer );
        return writer.toString();
    }

    @WsMethod( method = GET, path = "/ui/value", produces = "text/html" )
    public String value( @WsParam( from = QUERY ) String query, @WsParam( from = QUERY ) Optional<String> mode ) {
        String effectiveMode = mode.filter( m -> !m.isEmpty() ).orElse( "inspect" );
        VelocityContext context = new VelocityContext();
        context.put( "query", query );
        context.put( "mode", effectiveMode );
        context.put( "encodedQuery", URLEncoder.encode( query, StandardCharsets.UTF_8 ) );
        try {
            Object result = jPathWS.evaluatePath( query );
            context.put( "error", false );
            context.put( "json", Binder.json.marshal( result, true ) );

            boolean inspectable = result != null && !isLeaf( result.getClass() );
            context.put( "inspectable", inspectable );
            if( inspectable ) {
                putInspectTables( context, result, Reflect.reflect( result.getClass() ), query );
            }
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
            context.put( "error", true );
            context.put( "stackTrace", Throwables.getStackTraceAsString( e ) );
        }
        StringWriter writer = new StringWriter();
        engine.getTemplate( "oap/ws/admin/inspector-value.html.vm" ).merge( context, writer );
        return writer.toString();
    }

    private static boolean isLeaf( Class<?> type ) {
        return type.isPrimitive()
            || type == String.class
            || Number.class.isAssignableFrom( type )
            || type == Boolean.class
            || type == Character.class
            || type.getName().startsWith( "java." )
            || type.getName().startsWith( "javax." );
    }

    private void putInspectTables( VelocityContext context, Object instance, Reflection reflection, String queryPrefix ) {
        List<Map<String, String>> fields = reflection.fields.values().stream()
            .sorted()
            .map( f -> {
                Class<?> type = f.underlying.getType();
                String value = "";
                if( type.isPrimitive() || type == String.class ) {
                    try {
                        value = String.valueOf( f.get( instance ) );
                    } catch( Exception e ) {
                        value = "";
                    }
                }
                return Map.of(
                    "name", f.name(),
                    "modifier", Modifier.toString( f.underlying.getModifiers() ),
                    "type", type.getName(),
                    "query", queryPrefix + "." + f.name(),
                    "value", value
                );
            } )
            .collect( Collectors.toList() );
        context.put( "fields", fields );

        Set<Class<?>> jpathLiteralTypes = Set.of( String.class, int.class, Integer.class, long.class, Long.class );
        List<Map<String, Object>> methods = reflection.methods.stream()
            .filter( m -> m.underlying.getDeclaringClass() != Object.class )
            .filter( m -> m.parameters.stream().allMatch( p -> jpathLiteralTypes.contains( p.underlying.getType() ) ) )
            .collect( Collectors.toMap(
                m -> m.name() + "(" + m.parameters.stream()
                    .map( p -> p.underlying.getType().getName() )
                    .collect( Collectors.joining( "," ) ) + ")",
                m -> m,
                ( a, b ) -> a,
                LinkedHashMap::new
            ) )
            .values().stream()
            .sorted( Comparator.comparing( m -> m.name() ) )
            .map( m -> {
                List<Map<String, String>> params = m.parameters.stream()
                    .map( p -> Map.of(
                        "name", p.name(),
                        "kind", p.underlying.getType() == String.class ? "string" : "number"
                    ) )
                    .collect( Collectors.toList() );
                Map<String, Object> row = new LinkedHashMap<>();
                row.put( "name", m.name() );
                row.put( "modifier", Modifier.toString( m.underlying.getModifiers() ) );
                row.put( "returnType", m.underlying.getReturnType().getName() );
                row.put( "parameters", m.parameters.stream()
                    .map( p -> p.underlying.getType().getSimpleName() + " " + p.name() )
                    .collect( Collectors.joining( ", " ) ) );
                row.put( "zeroArg", m.parameters.isEmpty() );
                row.put( "query", m.parameters.isEmpty() ? queryPrefix + "." + m.name() + "()" : queryPrefix + "." + m.name() );
                row.put( "params", params );
                return row;
            } )
            .collect( Collectors.toList() );
        context.put( "methods", methods );
    }
}
