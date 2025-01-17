package com.optum.envvars.impl;

import com.optum.envvars.EnvVarsException;

public class SimpleEnvVar extends AbstractEnvVar {

    public SimpleEnvVar(String key, String value) throws EnvVarsException {
        super(key, value);
    }

    public String asJSON() {
        return "{ \"name\" : \"" + key + "\", \"value\" : \"" + getValueAsJSON() + "\" }";
    }

    @Override
    public String asConfigMap() {
        return key + "=" + value;
    }
}
