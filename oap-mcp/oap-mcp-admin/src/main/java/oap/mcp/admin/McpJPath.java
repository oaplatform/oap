/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp.admin;

import oap.mcp.annotations.McpPrompt;
import oap.mcp.annotations.McpPromptParam;
import oap.mcp.annotations.McpTool;
import oap.mcp.annotations.McpToolParam;
import oap.ws.admin.JPathWS;

import java.util.List;

import static dev.khbd.interp4j.core.Interpolations.s;

public class McpJPath {
    private final JPathWS jPathWS;

    public McpJPath( JPathWS jPathWS ) {
        this.jPathWS = jPathWS;
    }

    @McpTool( name = "listServices",
        description = "Lists kernel services matching a glob pattern. Use '*' for all services, '*name*' to filter by substring." )
    public String listServices(
        @McpToolParam( name = "pattern",
            description = "Glob pattern: '*' for all, '*suffix', 'prefix*', '*contains*'",
            required = false ) String pattern
    ) {
        String glob = pattern != null ? pattern : "*";
        List<String> services = jPathWS.listServices( glob );
        return String.join( "\n", services );
    }

    @McpTool( name = "queryService",
        description = "Evaluates a JPath expression against the kernel service tree. Query format: module.service.instance.field or module.service.instance.method()" )
    public String queryService(
        @McpToolParam( name = "query",
            description = "JPath expression, e.g. 'my-module.my-service.instance.someField'",
            required = true ) String query
    ) {
        Object result = jPathWS.evaluatePath( query );
        return result != null ? result.toString() : "null";
    }

    @McpPrompt( name = "inspectService",
        description = "Inspect a kernel service by partial name — finds matching services and queries their properties" )
    public String inspectService(
        @McpPromptParam( name = "service",
            description = "Partial service name or glob pattern to search for",
            required = true ) String service
    ) {
        return s( """
            You are an OAP application expert. Inspect the kernel service matching '${service}'.
            Steps:
            1. Call 'listServices' with pattern='*${service}*' to find all services whose name contains '${service}'.
            2. Present the matching service names to the user and ask them to confirm the exact one.
            3. Once confirmed, call 'queryService' with queries like '<module>.<service>.instance' to inspect its fields.
            4. Summarize the service configuration and state.""" );
    }
}
