package com.optum.envvars.impl;

import com.optum.envvars.EnvVarsException;

public class SecretEnvVar extends AbstractEnvVar {

    public SecretEnvVar(String key, String value) throws EnvVarsException {
        super(key, value);
    }

    public String asJSON() {
        return "{ \"name\" : \"" + key + "\", \"valueFrom\" : { \"secretKeyRef\" : { \"name\" : \"secretdata\", \"key\"  : \"" + getValueAsJSON() + "\"}}}";
    }

    @Override
    public String asConfigMap() {
        return null;
    }
}
