/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.util.Cuid;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.AbstractSecureWS;
import oap.ws.sso.User;
import oap.ws.sso.WsSecurity;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.OK;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.SESSION;
import static oap.ws.validate.ValidationErrors.error;

@Slf4j
public class McpWS extends AbstractSecureWS {
    private final McpService mcpService;

    public McpWS( McpService mcpService ) {
        this.mcpService = mcpService;
    }

    @WsMethod( path = "/sse", method = GET )
    @WsSecurity( permissions = { "admin:mcp" } )
    @WsValidate( { "systemAdminRole" } )
    public Response sse( @WsParam( from = SESSION ) Optional<User> loggedUser ) {
        String sessionId = Cuid.UNIQUE.next();

        log.debug( "new sse sessionId {} loggedUser {}", sessionId, loggedUser.get().getEmail() );

        McpService.McpSession session = mcpService.createSession( sessionId );

        Consumer<OutputStream> body = out -> {
            try {
                String endpoint = "event: endpoint\ndata: /mcp/message?sessionId=" + sessionId + "\n\n";
                out.write( endpoint.getBytes( StandardCharsets.UTF_8 ) );
                out.flush();

                log.debug( "init event {}", endpoint );

                mcpService.drainSse( session, out );
            } catch( Exception e ) {
                log.error( "SSE write error for session {}", sessionId, e );
                mcpService.closeSession( sessionId );
            }
        };

        return new Response( OK )
            .withContentType( "text/event-stream" )
            .withHeader( "Cache-Control", "no-cache" )
            .withHeader( "Connection", "keep-alive" )
            .withBody( body, true );
    }

    @WsMethod( path = "/message", method = POST, raw = true )
    @WsSecurity( permissions = { "admin:mcp" } )
    @WsValidate( { "systemAdminRole" } )
    public Response message( @WsParam( from = QUERY, name = "sessionId" ) Optional<String> sessionIdOpt,
                             @WsParam( from = BODY ) String body,
                             @WsParam( from = SESSION ) Optional<User> loggedUser ) {

        log.debug( "MCP message session {} body {} loggedUser {}", sessionIdOpt, body, loggedUser.get().getEmail() );

        String sessionId = sessionIdOpt.orElse( null );


        if( sessionId == null ) {
            return new Response( Http.StatusCode.BAD_REQUEST, "BAD_REQUEST",
                Http.ContentType.APPLICATION_JSON,
                McpError.builder( McpSchema.ErrorCodes.METHOD_NOT_FOUND )
                    .message( "Session ID missing in message endpoint" )
                    .build() );
        }

        McpService.McpSession session = mcpService.getSession( sessionId );

        if( session == null ) {
            return new Response( Http.StatusCode.NOT_FOUND, "NOT_FOUND",
                Http.ContentType.APPLICATION_JSON,
                McpError.builder( McpSchema.ErrorCodes.METHOD_NOT_FOUND )
                    .message( "Session ID missing in message endpoint" )
                    .build() );
        }


        try {
            mcpService.dispatchMessage( session, body );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );

            return new Response( Http.StatusCode.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                Http.ContentType.APPLICATION_JSON,
                McpError.builder( McpSchema.ErrorCodes.INTERNAL_ERROR )
                    .message( e.getMessage() )
                    .build() );
        }

        return Response.ok();
    }

    public ValidationErrors systemAdminRole( Optional<User> loggedUser ) {
        if( loggedUser.isEmpty() || !isSystem( loggedUser.get() ) ) {
            return error( FORBIDDEN, "Only System ADMIN can access to this api" );
        } else return ValidationErrors.empty();
    }
}
