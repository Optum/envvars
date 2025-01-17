package com.optum.envvars.mapdata.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.schema.EnvVarsSchemaReader;
import com.optum.envvars.set.EnvVarsStaticSets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CachingJsonEnvVarsMapDataReader {
    private CachingJsonEnvVarsMapDataReader() {
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
                throw new EnvVarsException("Unable to open JSON resource " + resourcename + ".  Probably due to resource not found.");
            }
            JsonFactory factory = new JsonFactory();
            try {
                JsonParser jsonParser = factory.createParser(new InputStreamReader(is));
                Map globalScope = new ObjectMapper().readValue(jsonParser, HashMap.class);
                envVarMapDataEngine = new EnvVarsMapDataEngine(envVarsSchemaReader, envVarsStaticSets, globalScope);
                globalCache.put(resourcename, envVarMapDataEngine);
            } catch (IOException e) {
                throw new EnvVarsException("Unable to load JSON resource " + resourcename, e);
            }
        }
        return envVarMapDataEngine.get(envVarsRuntimeSelectors);
    }
}
