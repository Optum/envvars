package com.optum.envvars.key;

import com.optum.envvars.EnvVarsException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapKeySourceOfTruth implements KeySourceOfTruth {
    private final String sourceName;
    private final Map<Object, Object> globalScope;
    private final AggregateKeySourceOfTruth contextDirectory;

    public MapKeySourceOfTruth(String sourceName, Map<Object, Object> globalScope) {
        this.sourceName = sourceName;
        this.globalScope = globalScope;
        this.contextDirectory = new AggregateKeySourceOfTruth();
    }

    public EnvVarsKeys registerKeys(String context) throws EnvVarsException {
        // Gather
        Set<String> keys = new HashSet<>();
        Object rawResult = globalScope.get(context);
        if (rawResult==null) {
            throw new EnvVarsException("KeySourceOfTruth [" + sourceName + "] Context:" + context + " missing.");
        }
        if (!(rawResult instanceof List)) {
            throw new EnvVarsException("KeySourceOfTruth [" + sourceName + "] Context:" + context + " expected to be a List but was a " + rawResult.getClass().getCanonicalName() + ".");
        }
        List<Object> list = (List)rawResult;
        for(Object item : list) {
            if (item==null) {
                throw new EnvVarsException("KeySourceOfTruth [" + sourceName + "] Context:" + context + " key is null.");
            }
            if (!(item instanceof String)) {
                throw new EnvVarsException("KeySourceOfTruth [" + sourceName + "] Context:" + context + " key expected to be a String but was a " + item.getClass().getCanonicalName());
            }
            keys.add((String)item);
        }
        EnvVarsKeys envVarsKeys = new EnvVarsKeys(context, keys);
        contextDirectory.addKeys(envVarsKeys);
        return envVarsKeys;
    }

    @Override
    public EnvVarsKeys getKeys(String context) {
        return contextDirectory.getKeys(context);
    }

    @Override
    public Collection<EnvVarsKeys> getAllRegisteredKeys() {
        return contextDirectory.getAllRegisteredKeys();
    }

    @Override
    public void throwExceptionIfForeignKeysNotFound(String context, Set<String> keys) throws EnvVarsException {
        contextDirectory.throwExceptionIfForeignKeysNotFound(context, keys);
    }

}
