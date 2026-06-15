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

import static dev.khbd.interp4j.core.Interpolations.s;

public class McpLog {
    private final LogWS logWS;

    public McpLog( LogWS logWS ) {
        this.logWS = logWS;
    }

    @McpTool( name = "getLoggers", description = "Returns all loggers with their current log levels" )
    public String getLoggers() {
        Map<String, String> loggers = logWS.getAll();
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
        @McpPromptParam( name = "package", description = "Java package name to diagnose", required = true ) String pkg
    ) {
        return s( """
            You are a Java application expert. Diagnose the logging configuration for package '${pkg}'.
            Steps:
            1. Use 'getLoggers' to check the current level for '${pkg}'.
            2. If no explicit level is set, explain that it inherits from its parent logger.
            3. Suggest an appropriate log level for debugging vs. production use.
            4. Use 'setLogLevel' if a level change is needed.""" );
    }
}
