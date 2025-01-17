package com.optum.envvars.schema.yaml;

import com.optum.envvars.schema.EnvVarsDefinitions;
import com.optum.envvars.schema.EnvVarsNodeType;
import com.optum.envvars.schema.EnvVarsVariableFilter;

import java.util.Map;

public class DefinitionsYaml {
    public Map<String, NodeTypesYaml> node_types;
    public Map<String, VariableFilterYaml> variable_filters;

    public EnvVarsDefinitions build() {
        EnvVarsDefinitions envVarsDefinitions = new EnvVarsDefinitions();

        if (node_types!=null) {
            for (Map.Entry<String, NodeTypesYaml> entry : node_types.entrySet()) {
                final String key = entry.getKey();
                final NodeTypesYaml nodeTypesYaml = entry.getValue();
                EnvVarsNodeType nodeType = new EnvVarsNodeType(nodeTypesYaml.name);
                envVarsDefinitions.put(key, nodeType);
            }
        }

        if (variable_filters!=null) {
            for (Map.Entry<String, VariableFilterYaml> entry : variable_filters.entrySet()) {
                final String key = entry.getKey();
                final VariableFilterYaml vfy = entry.getValue();
                EnvVarsVariableFilter envVarsVariableFilter = new EnvVarsVariableFilter(vfy.name, vfy.regex, vfy.description, vfy.usage);
                envVarsDefinitions.put(key, envVarsVariableFilter);
            }
        }
        return envVarsDefinitions;
    }

}
