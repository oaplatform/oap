/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import oap.application.annotation.Start;
import oap.mcp.annotations.McpPrompt;
import oap.mcp.annotations.McpPromptParam;
import oap.mcp.annotations.McpTool;
import oap.mcp.annotations.McpToolParam;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static dev.khbd.interp4j.core.Interpolations.s;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class McpService implements McpServerTransportProvider, Closeable {
    public static final String MESSAGE_EVENT_TYPE = "message";
    static final String SSE_CLOSE = "__CLOSE__";
    public final ArrayList<Object> tools = new ArrayList<>();
    public final String name;
    public final String version;
    private final McpJsonMapper jsonMapper;
    private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();
    private McpSyncServer server;
    private volatile McpServerSession.Factory sessionFactory;

    public McpService( String name, String version ) {
        this.name = name;
        this.version = version;
        this.jsonMapper = McpJsonDefaults.getMapper();
    }

    private static List<McpServerFeatures.SyncPromptSpecification> annotatedPromptSpecs( Object obj ) {
        return Arrays.stream( obj.getClass().getDeclaredMethods() )
            .filter( m -> m.isAnnotationPresent( McpPrompt.class ) )
            .sorted( Comparator.comparing( Method::getName ) )
            .map( m -> toPromptSpec( m, obj ) )
            .toList();
    }

    private static McpServerFeatures.SyncPromptSpecification toPromptSpec( Method method, Object obj ) {
        McpPrompt ann = method.getAnnotation( McpPrompt.class );
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        String description = ann.description().isEmpty() ? name : ann.description();

        List<McpSchema.PromptArgument> arguments = Arrays.stream( method.getParameters() )
            .map( p -> p.getAnnotation( McpPromptParam.class ) )
            .filter( p -> p != null )
            .map( p -> new McpSchema.PromptArgument( p.name(), p.description(), p.required() ) )
            .toList();

        McpSchema.Prompt prompt = new McpSchema.Prompt( name, description, arguments );

        return new McpServerFeatures.SyncPromptSpecification( prompt, ( exchange, request ) -> {
            Object[] args = Arrays.stream( method.getParameters() )
                .map( p -> {
                    McpPromptParam pp = p.getAnnotation( McpPromptParam.class );
                    return pp != null ? request.arguments().get( pp.name() ) : null;
                } )
                .toArray();
            try {
                String text = ( String ) method.invoke( obj, args );
                McpSchema.PromptMessage message = new McpSchema.PromptMessage(
                    McpSchema.Role.USER, new McpSchema.TextContent( text ) );
                return new McpSchema.GetPromptResult( description, List.of( message ) );
            } catch( InvocationTargetException e ) {
                throw new RuntimeException( e.getCause() );
            } catch( IllegalAccessException e ) {
                throw new RuntimeException( e );
            }
        } );
    }

    private static List<McpServerFeatures.SyncToolSpecification> annotatedToolSpecs( Object tools ) {
        return Arrays.stream( tools.getClass().getDeclaredMethods() )
            .filter( m -> m.isAnnotationPresent( McpTool.class ) )
            .sorted( Comparator.comparing( Method::getName ) )
            .map( m -> toSpec( m, tools ) )
            .toList();
    }

    private static McpServerFeatures.SyncToolSpecification toSpec( Method method, Object tools ) {
        McpTool ann = method.getAnnotation( McpTool.class );
        String name = ann.name().isEmpty() ? method.getName() : ann.name();
        String description = ann.description().isEmpty() ? name : ann.description();

        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for( Parameter param : method.getParameters() ) {
            McpToolParam p = param.getAnnotation( McpToolParam.class );
            if( p != null ) {
                props.put( p.name(), Map.of( "type", "string" ) );
                if( p.required() ) required.add( p.name() );
            }
        }
        McpSchema.JsonSchema schema = new McpSchema.JsonSchema( "object", props, required, null, null, null );
        McpSchema.Tool tool = McpSchema.Tool.builder().name( name ).description( description ).inputSchema( schema ).build();

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool( tool )
            .callHandler( ( exchange, request ) -> {
                Object[] args = Arrays.stream( method.getParameters() )
                    .map( p -> {
                        McpToolParam tp = p.getAnnotation( McpToolParam.class );
                        return tp != null ? request.arguments().get( tp.name() ) : null;
                    } )
                    .toArray();
                try {
                    Object result = method.invoke( tools, args );
                    return McpSchema.CallToolResult.builder()
                        .addTextContent( result != null ? result.toString() : "" )
                        .build();
                } catch( InvocationTargetException e ) {
                    throw new RuntimeException( e.getCause() );
                } catch( IllegalAccessException e ) {
                    throw new RuntimeException( e );
                }
            } )
            .build();
    }

    @Start
    public void start() {
        this.server = buildServer();
    }

    @Override
    public void setSessionFactory( McpServerSession.Factory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients( String method, Object params ) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable( () -> {
            sessions.values().forEach( s -> s.queue().offer( SSE_CLOSE ) );
            sessions.clear();
        } );
    }

    public McpSession createSession( String sessionId ) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        McpServerSession session = sessionFactory.create( new QueueBackedTransport( queue, jsonMapper ) );
        McpSession mcpSession = new McpSession( queue, session );
        sessions.put( sessionId, mcpSession );
        log.info( "MCP session created: {}", sessionId );
        return mcpSession;
    }

    public McpSession getSession( String sessionId ) {
        return sessions.get( sessionId );
    }

    public void dispatchMessage( McpSession mcpSession, String body ) throws IOException {
        McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage( jsonMapper, body );
        mcpSession.session().handle( message ).block();
    }

    public void drainSse( McpSession mcpSession, OutputStream out ) {
        try {
            while( true ) {
                String event = mcpSession.queue().take();

                log.debug( "mcp event {}", event );

                if( SSE_CLOSE.equals( event ) ) break;

                out.write( s( "event: ${MESSAGE_EVENT_TYPE}\n" ).getBytes( UTF_8 ) );
                out.write( s( "data: ${event}\n\n" ).getBytes( UTF_8 ) );

                out.flush();
            }
        } catch( Exception e ) {
            log.debug( "MCP SSE stream ended for session {}: {}", mcpSession.session.getId(), e.getMessage() );
        } finally {
            sessions.remove( mcpSession.session.getId() );
            log.info( "MCP session closed: {}", mcpSession.session.getId() );
        }
    }

    public void closeSession( String sessionId ) {
        McpSession mcpSession = sessions.remove( sessionId );
        if( mcpSession != null ) mcpSession.queue().offer( SSE_CLOSE );
    }

    @Override
    public void close() {
        server.closeGracefully();
    }

    private McpSyncServer buildServer() {
        return McpServer.sync( this )
            .serverInfo( name, version )
            .capabilities( McpSchema.ServerCapabilities.builder().tools( true ).prompts( true ).build() )
            .tools( tools.stream().flatMap( t -> annotatedToolSpecs( t ).stream() ).toList() )
            .prompts( tools.stream().flatMap( t -> annotatedPromptSpecs( t ).stream() ).toList() )
            .build();
    }

    public record McpSession( BlockingQueue<String> queue, McpServerSession session ) {}

    private static class QueueBackedTransport implements McpServerTransport {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( QueueBackedTransport.class );

        private final BlockingQueue<String> queue;
        private final McpJsonMapper jsonMapper;

        QueueBackedTransport( BlockingQueue<String> queue, McpJsonMapper jsonMapper ) {
            this.queue = queue;
            this.jsonMapper = jsonMapper;
        }

        @Override
        public Mono<Void> sendMessage( McpSchema.JSONRPCMessage message ) {
            return Mono.fromCallable( () -> {
                queue.offer( jsonMapper.writeValueAsString( message ) );
                return null;
            } );
        }

        @Override
        public <T> T unmarshalFrom( Object data, TypeRef<T> typeRef ) {
            return jsonMapper.convertValue( data, typeRef );
        }

        @Override
        public Mono<Void> closeGracefully() {
            queue.offer( McpService.SSE_CLOSE );
            return Mono.empty();
        }
    }
}
