package com.optum.envvars.impl;

import com.optum.envvars.EnvVarsException;

public class ConfigMapEnvVar extends AbstractEnvVar {

    public ConfigMapEnvVar(String key, String value) throws EnvVarsException {
        super(key, value);
    }

    public String asJSON() {
        return "{ \"name\" : \"" + key + "\", \"valueFrom\" : { \"configMapKeyRef\" : { \"name\" : \"envvardata\", \"key\"  : \"" + getValueAsJSON() + "\"}}}";
    }

    @Override
    public String asConfigMap() {
        return null;
    }
}
