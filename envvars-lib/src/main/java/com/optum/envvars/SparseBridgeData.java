package com.optum.envvars;

import java.util.HashMap;
import java.util.Map;

public class SparseBridgeData {
    final Map<String, String> cachedResults = new HashMap<>();
    final EnvVarsEngine envVarsEngine;

    public SparseBridgeData(EnvVarsEngine envVarsEngine) {
        this.envVarsEngine = envVarsEngine;
    }

    public String get(String key) throws EnvVarsException {
        final String cachedValue = cachedResults.get(key);
        if (cachedValue!=null) {
            return cachedValue;
        }
        final String value = envVarsEngine.generateBridgeValue(key);
        if (value!=null) {
            cachedResults.put(key, value);
        }
        return value;
    }

}
