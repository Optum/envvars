package com.optum.envvars.impl;

import com.optum.envvars.EnvVarsException;

public class ReferenceEnvVar extends AbstractEnvVar {

    public ReferenceEnvVar(String key, String value) throws EnvVarsException {
        super(key, value);
    }

    @Override
    public boolean isSecret() {
        return false;
    }

    @Override
    public boolean isReference() {
        return true;
    }
}
