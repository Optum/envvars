package com.optum.envvars.mapdata.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.schema.EnvVarsSchemaReader;
import com.optum.envvars.set.EnvVarsStaticSets;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class CachingYamlEnvVarsMapDataReader {
    private CachingYamlEnvVarsMapDataReader() {
    }

    public static EnvVarsMapData fromResource(Map<String, EnvVarsMapDataEngine> globalCache, ClassLoader classLoader, String resourcename, EnvVarsRuntimeSelectors envVarsRuntimeSelectors)
            throws EnvVarsException {
        return fromResource(globalCache, classLoader, resourcename, envVarsRuntimeSelectors, null, null);
    }

    public static EnvVarsMapData fromResource(Map<String, EnvVarsMapDataEngine> globalCache, ClassLoader classLoader, String resourcename, EnvVarsRuntimeSelectors envVarsRuntimeSelectors, EnvVarsSchemaReader envVarsSchemaReader, EnvVarsStaticSets envVarsStaticSets)
            throws EnvVarsException {
        EnvVarsMapDataEngine envVarMapDataEngine = globalCache.get(resourcename);
        if (envVarMapDataEngine==null) {
            InputStream is = classLoader.getResourceAsStream(resourcename);
            if (is == null) {
                throw new EnvVarsException("Unable to open YAML resource " + resourcename + ".  Probably due to resource not found.");
            }
            SnakeYamlLoaderWrapper snakeYamlLoaderWrapper = new InputStreamSnakeYamlLoaderWrapper(is);
            Map<Object, Object> globalScope = snakeYamlLoaderWrapper.load(new Yaml());
            envVarMapDataEngine = new EnvVarsMapDataEngine(envVarsSchemaReader, envVarsStaticSets, globalScope);
            globalCache.put(resourcename, envVarMapDataEngine);
        }
        return envVarMapDataEngine.get(envVarsRuntimeSelectors);
    }

}
