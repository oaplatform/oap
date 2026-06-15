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
import oap.ws.admin.LogWS;

import java.util.Map;
import java.util.Optional;

import static dev.khbd.interp4j.core.Interpolations.s;

public class McpLog {
    private final LogWS logWS;

    public McpLog( LogWS logWS ) {
        this.logWS = logWS;
    }

    @McpTool( name = "getLoggers",
        description = "Returns loggers with their current log levels. Pass all=true to include loggers inheriting from root." )
    public String getLoggers(
        @McpToolParam( name = "all",
            description = "Set to 'true' or 'yes' to include loggers inheriting their level from root",
            required = false ) String all
    ) {
        Map<String, String> loggers = logWS.getAll( Optional.ofNullable( all ) );
        StringBuilder sb = new StringBuilder();
        loggers.forEach( ( name, level ) -> sb.append( name ).append( " = " ).append( level ).append( '\n' ) );
        return sb.toString();
    }

    @McpTool( name = "setLogLevel", description = "Sets the log level for a Java package" )
    public void setLogLevel(
        @McpToolParam( name = "package", description = "Java package name", required = true ) String pkg,
        @McpToolParam( name = "level", description = "Log level: TRACE, DEBUG, INFO, WARN, ERROR", required = true ) String level
    ) {
        logWS.setLevel( level, pkg );
    }

    @McpTool( name = "resetLogging", description = "Reloads logback.xml and resets all log levels to their defaults" )
    public void resetLogging() {
        logWS.reset();
    }

    @McpPrompt( name = "analyzeLogging", description = "Analyze the current logging configuration" )
    public String analyzeLogging() {
        return """
            You are a Java application expert. Analyze the active logging configuration.
            Use the 'getLoggers' tool to retrieve the current loggers and their levels.
            Identify any loggers set to verbose levels (TRACE/DEBUG) that may impact performance \
            in production, and suggest appropriate adjustments. \
            Also highlight any important packages that have no explicit level set.""";
    }

    @McpPrompt( name = "diagnosePackage", description = "Diagnose logging for a specific Java package" )
    public String diagnosePackage(
        @McpPromptParam( name = "package", description = "Java package name or partial name to diagnose", required = true ) String pkg
    ) {
        return s( """
            You are a Java application expert. Diagnose the logging configuration for package '${pkg}'.
            Steps:
            1. Call 'getLoggers' with all='true' to retrieve all loggers including those inheriting from root.
            2. Filter the result to find all logger names that contain '${pkg}' as a substring.
            3. Present the matching logger names and their current effective levels to the user.
            4. Ask the user to confirm which exact logger name they want to target.
            5. Once confirmed, suggest the appropriate log level and offer to call 'setLogLevel' with the confirmed name.""" );
    }
}
