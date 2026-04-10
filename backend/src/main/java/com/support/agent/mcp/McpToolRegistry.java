package com.support.agent.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class McpToolRegistry {

    private final Map<String, McpTool> tools;

    /**
     * Spring injects every @Component that implements McpTool.
     * No manual registration needed — just add a new tool class.
     */
    @Autowired
    public McpToolRegistry(List<McpTool> allTools) {
        this.tools = allTools.stream()
                .collect(Collectors.toMap(
                        McpTool::name,
                        Function.identity()
                ));
    }

    /** Returns the tool schemas Claude sees in every API request. */
    public List<Map<String, Object>> getSchemas() {
        return tools.values().stream()
                .map(McpTool::schema)
                .toList();
    }

    /** Dispatches a tool call by name. Throws if the tool is unknown. */
    public Object invoke(String name, Map<String, Object> args) {
        return Optional.ofNullable(tools.get(name))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown tool: " + name))
                .execute(args);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
