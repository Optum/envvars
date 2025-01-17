package com.optum.envvars.mapdata.source;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapData;
import com.optum.envvars.set.EnvVarsStaticSets;

public interface EnvVarMapDataSource {
    EnvVarsMapData getEnvVarMapData(EnvVarsStaticSets envVarsStaticSets) throws EnvVarsException;
}
