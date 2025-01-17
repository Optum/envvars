package com.optum.envvars.set;

import com.optum.envvars.EnvVarsException;

public interface EnvVarsSetsReader {
    EnvVarsStaticSets buildFromString(String schemaString, String definitionsString) throws EnvVarsException;
    EnvVarsStaticSets buildFromResource(String filename) throws EnvVarsException;
}
