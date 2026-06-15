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
import oap.http.Http;
import oap.jpath.JPath;
import oap.jpath.NullPointer;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.khbd.interp4j.core.Interpolations.s;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class JPathWS {
    private final Kernel kernel;

    public JPathWS( Kernel kernel ) {
        this.kernel = kernel;
    }

    public List<String> listServices( String pattern ) {
        String regex = Arrays.stream( pattern.split( "\\*", -1 ) )
            .map( Pattern::quote )
            .collect( Collectors.joining( ".*" ) );
        return kernel.services.moduleMap.entrySet().stream()
            .flatMap( module -> module.getValue().keySet().stream()
                .map( svc -> module.getKey() + "." + svc ) )
            .filter( name -> name.matches( regex ) )
            .sorted()
            .collect( Collectors.toList() );
    }

    @SuppressWarnings( "unchecked" )
    public Object evaluatePath( String query ) {
        AtomicReference<Object> result = new AtomicReference<>();
        JPath.evaluate( s( "$${${query}}" ), ( Map<String, Object> ) ( Object ) kernel.services.moduleMap,
            pointer -> result.set( pointer.get() ) );
        return result.get();
    }

    @SuppressWarnings( "unchecked" )
    @WsMethod( method = GET, path = "/" )
    public Response get( @WsParam( from = QUERY ) String query ) {
        log.debug( "query = {}", query );
        try {
            if( query.contains( "*" ) ) {
                return Response.jsonOk().withBody( listServices( query ), false );
            }

            AtomicReference<Object> result = new AtomicReference<>();

            String[] fields = StringUtils.split( query, '.' );

            if( fields.length > 1 ) {
                JPath.evaluate( s( "$${${fields[0]}.${fields[1]}}" ), ( Map<String, Object> ) ( Object ) kernel.services.moduleMap, pointer -> {
                    if( !( pointer instanceof NullPointer ) ) {
                        result.set( fields[1] );
                    }
                } );
            }
            if( result.get() == null ) {
                return new Response( Http.StatusCode.BAD_REQUEST ).withBody( s( "unknown module service ${fields[0]}.${fields[1]}" ) );
            }

            if( fields.length > 0 ) {
                JPath.evaluate( s( "$${${fields[0]}}" ), ( Map<String, Object> ) ( Object ) kernel.services.moduleMap, pointer -> {
                    if( !( pointer instanceof NullPointer ) ) {
                        result.set( fields[0] );
                    }
                } );
            }

            if( result.get() == null ) {
                return new Response( Http.StatusCode.BAD_REQUEST ).withBody( "unknown module " + fields[0] );
            }
            result.set( null );

            result.set( null );

            return Response.jsonOk().withBody( evaluatePath( query ), false );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
            return new Response( Http.StatusCode.BAD_REQUEST, e.getMessage(), Http.ContentType.TEXT_PLAIN ).withBody( Throwables.getStackTraceAsString( e ), true );
        }
    }
}
