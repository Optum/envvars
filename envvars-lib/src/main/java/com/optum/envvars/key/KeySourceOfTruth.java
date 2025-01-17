package com.optum.envvars.key;

import com.optum.envvars.EnvVarsException;

import java.util.Collection;
import java.util.Set;

public interface KeySourceOfTruth {
    EnvVarsKeys getKeys(String context);
    Collection<EnvVarsKeys> getAllRegisteredKeys();
    void throwExceptionIfForeignKeysNotFound(String context, Set<String> keys) throws EnvVarsException;
}
