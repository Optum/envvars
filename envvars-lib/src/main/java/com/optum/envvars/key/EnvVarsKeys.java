package com.optum.envvars.key;

import com.optum.envvars.EnvVarsException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnvVarsKeys {
    private final String context;
    private final Set<String> keys;

    public EnvVarsKeys(String context, Set<String> keys) {
        this.context = context;
        if (keys==null) {
            throw new IllegalArgumentException("Null key set is not allowed for the creation of an EnvVarsKeys.");
        }
        Set<String> defensiveCopy = new HashSet<>(keys);
        this.keys = Collections.unmodifiableSet(defensiveCopy);
    }

    public String getContext() {
        return context;
    }

    public Set<String> getKeys() throws EnvVarsException {
        return keys;
    }

    public void throwExceptionIfForeignKeyNotFound(String key) throws EnvVarsException {
        if (!keys.contains(key)) {
            throw new EnvVarsException("Context:" + context + " is not valid for key:" + key + ".  Is key mistyped or retired?");
        }
    }
}
