package com.optum.envvars.mapdata;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.schema.EnvVarsRuntimeSelectors;
import com.optum.envvars.set.EnvVarsStaticSets;

public interface EnvVarsMapDataReader {
    EnvVarsMapData getEnvVarMapData(EnvVarsStaticSets envVarsStaticSets, EnvVarsRuntimeSelectors envVarsRuntimeSelectors) throws EnvVarsException;
}
