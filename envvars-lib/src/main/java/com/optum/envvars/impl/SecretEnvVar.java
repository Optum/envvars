package com.optum.envvars.impl;

import com.optum.envvars.EnvVarsException;

public class SecretEnvVar extends AbstractEnvVar {

    public SecretEnvVar(String key, String value) throws EnvVarsException {
        super(key, value);
    }

    @Override
    public boolean isSecret() {
        return true;
    }

    @Override
    public boolean isReference() {
        return false;
    }
}
