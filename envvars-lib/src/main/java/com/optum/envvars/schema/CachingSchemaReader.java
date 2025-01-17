package com.optum.envvars.schema;

import com.optum.envvars.EnvVarsException;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CachingSchemaReader implements EnvVarsSchemaReader {
    private final EnvVarsSchemaReader source;
    private Map<URL, EnvVarsStaticSchema> cache = new HashMap<>();

    public CachingSchemaReader(EnvVarsSchemaReader source) {
        this.source = source;
    }

    @Override
    public EnvVarsStaticSchema buildFromString(String schemaString, String definitionsString) throws EnvVarsException {
        return source.buildFromString(schemaString, definitionsString);
    }

    @Override
    public EnvVarsStaticSchema buildFromURL(URL url) throws EnvVarsException {
        EnvVarsStaticSchema envVarsStaticSchema = cache.get(url);
        if (envVarsStaticSchema!=null) {
            return envVarsStaticSchema;
        }
        envVarsStaticSchema = source.buildFromURL(url);
        cache.put(url, envVarsStaticSchema);
        return envVarsStaticSchema;
    }
}
