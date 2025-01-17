package com.optum.envvars.mapdata.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.mapdata.EnvVarsMapDataReader;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.schema.yaml.YamlEnvVarsSchemaReader;
import com.optum.envvars.set.EnvVarsStaticSets;

import java.util.Map;

public class YamlEnvVarsMapDataReader implements EnvVarsMapDataReader {
    private final YamlLoadWrapper<Map<Object, Object>> yamlLoadWrapper;
    private final YamlEnvVarsSchemaReader yamlEnvVarsSchemaReader;

    public YamlEnvVarsMapDataReader(YamlLoadWrapper<Map<Object, Object>> yamlLoadWrapper) {
        this.yamlLoadWrapper = yamlLoadWrapper;
        this.yamlEnvVarsSchemaReader = new YamlEnvVarsSchemaReader();
    }

    @Override
    public EnvVarsMapData getEnvVarMapData(EnvVarsStaticSets envVarsStaticSets, EnvVarsRuntimeSelectors envVarsRuntimeSelectors) throws EnvVarsException {
        // Gather
        Map<Object, Object> globalScope = yamlLoadWrapper.load();

        EnvVarsMapDataEngine envVarMapDataEngine = new EnvVarsMapDataEngine(yamlEnvVarsSchemaReader, envVarsStaticSets, globalScope);
        return envVarMapDataEngine.get(envVarsRuntimeSelectors);
    }

}
