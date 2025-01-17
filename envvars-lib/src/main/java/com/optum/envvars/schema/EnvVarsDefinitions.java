package com.optum.envvars.schema;

import java.util.HashMap;
import java.util.Map;

public class EnvVarsDefinitions {
    private final Map<String, EnvVarsNodeType> nodeTypes = new HashMap<>();
    private final Map<String, EnvVarsVariableFilter> variableFilters = new HashMap<>();

    public EnvVarsDefinitions() {
    }

    public void put(EnvVarsDefinitions envVarsDefinitions) {
        nodeTypes.putAll(envVarsDefinitions.nodeTypes);
        variableFilters.putAll(envVarsDefinitions.variableFilters);
    }

    public void put(String key, EnvVarsNodeType nodeType) {
        nodeTypes.put(key, nodeType);
    }

    public void put(String key, EnvVarsVariableFilter envVarsVariableFilter) {
        variableFilters.put(key, envVarsVariableFilter);
    }

    public int getNodeTypesCount() {
        return nodeTypes.size();
    }

    public int getVariableFiltersCount() {
        return variableFilters.size();
    }

    public EnvVarsNodeType getNodeType(String key) {
        return nodeTypes.get(key);
    }

    public EnvVarsVariableFilter getVariableFilter(String key) {
        return variableFilters.get(key);
    }
}
