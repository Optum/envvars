package com.optum.envvars.mapdata.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.mapdata.EnvVarsMapDataEngine;
import com.optum.envvars.mapdata.EnvVarsMapDataReader;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.set.EnvVarsStaticSets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class JsonEnvVarsMapDataReader implements EnvVarsMapDataReader {
    private final JsonParser jsonParser;
    private final String sourceName;

    /**
     * EnvVars read from JSON needs a Jackson JsonParser and a sourceName (used solely as a reference to make
     * exception messages more helpful.)  If you have a particular method of creating your JsonParser, feel free
     * to use this constructor directly.  If, on the other hand, you don't want to bother with the details and one of
     * the static factory methods works for you, go ahead and use them.
     * @param jsonParser
     * @param sourceName
     */
    public JsonEnvVarsMapDataReader(JsonParser jsonParser, String sourceName) {
        this.jsonParser = jsonParser;
        this.sourceName = sourceName;
    }

    /**
     * Convenience Factory: Read Json from a standard File with name filename
     * @param filename
     * @throws EnvVarsException
     */
    public static JsonEnvVarsMapDataReader fromFile(String filename) throws EnvVarsException {
        JsonFactory factory = new JsonFactory();
        try {
            JsonParser jsonParser = factory.createParser(new File(filename));
            return new JsonEnvVarsMapDataReader(jsonParser, filename);
        } catch (IOException e) {
            throw new EnvVarsException("Unable to load JSON file " + filename, e);
        }
    }

    /**
     * Convenience Factory: Read Json from a ClassLoader Resource with name resourceName
     * @param classLoader
     * @param resourcename
     * @throws EnvVarsException
     */
    public static JsonEnvVarsMapDataReader fromResource(ClassLoader classLoader, String resourcename) throws EnvVarsException {
        InputStream is = classLoader.getResourceAsStream(resourcename);
        if (is==null) {
            throw new EnvVarsException("Unable to open JSON resource " + resourcename + ".  Probably due to resource not found.");
        }
        JsonFactory factory = new JsonFactory();
        try {
            JsonParser jsonParser = factory.createParser(new InputStreamReader(is));
            return new JsonEnvVarsMapDataReader(jsonParser, resourcename);
        } catch (IOException e) {
            throw new EnvVarsException("Unable to load JSON resource " + resourcename, e);
        }
    }

    @Override
    public EnvVarsMapData getEnvVarMapData(EnvVarsStaticSets envVarsStaticSets, EnvVarsRuntimeSelectors envVarsRuntimeSelectors) throws EnvVarsException {
        try {
            Map globalScope = new ObjectMapper().readValue(jsonParser, HashMap.class);
            EnvVarsMapDataEngine envVarMapDataEngine = new EnvVarsMapDataEngine(null, envVarsStaticSets, globalScope);
            return envVarMapDataEngine.get(envVarsRuntimeSelectors);
        } catch (IOException e) {
            throw new EnvVarsException("Unable to load JSON from " + sourceName, e);
        }
    }

}
