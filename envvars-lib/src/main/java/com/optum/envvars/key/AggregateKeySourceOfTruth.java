package com.optum.envvars.key;

import com.optum.envvars.EnvVarsException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AggregateKeySourceOfTruth implements KeySourceOfTruth {
    private final Map<String, EnvVarsKeys> contextDirectory;

    public AggregateKeySourceOfTruth() {
        this.contextDirectory = new HashMap<>();
    }

    public void addKeys(EnvVarsKeys keys) {
        contextDirectory.put(keys.getContext(), keys);
    }

    public void addAllRegisteredKeys(KeySourceOfTruth keySourceOfTruth) {
        for(EnvVarsKeys envVarsKeys : keySourceOfTruth.getAllRegisteredKeys()) {
            contextDirectory.put(envVarsKeys.getContext(), envVarsKeys);
        }
    }

    @Override
    public void throwExceptionIfForeignKeysNotFound(String context, Set<String> keys) throws EnvVarsException {
        EnvVarsKeys primaryKeys = contextDirectory.get(context);
        if (primaryKeys==null) {
            throw new EnvVarsException("No Foreign Keys for Context:" + context);
        }
        for(String key : keys) {
            primaryKeys.throwExceptionIfForeignKeyNotFound(key);
        }
    }

    @Override
    public EnvVarsKeys getKeys(String context) {
        return contextDirectory.get(context);
    }

    @Override
    public Collection<EnvVarsKeys> getAllRegisteredKeys() {
        return contextDirectory.values();
    }
}
