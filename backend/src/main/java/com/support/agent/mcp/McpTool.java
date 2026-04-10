package com.support.agent.mcp;

import java.util.List;
import java.util.Map;

/**
 * Contract every MCP tool must implement.
 * schema() is what Claude sees; execute() is what runs.
 */
public interface McpTool {

    String name();

    String description();

    /** JSON Schema for the tool's input — sent to Claude as the tool definition. */
    Map<String, Object> inputSchema();

    /** Execute the tool and return a serialisable result. */
    Object execute(Map<String, Object> args);

    /** Assembles the full tool descriptor expected by the Anthropic API. */
    default Map<String, Object> schema() {
        return Map.of(
                "name",         name(),
                "description",  description(),
                "input_schema", inputSchema()
        );
    }
}
