package com.optum.envvars.impl;

import com.optum.envvars.EnvVar;
import com.optum.envvars.EnvVarsException;

public abstract class AbstractEnvVar implements EnvVar, Comparable<EnvVar> {
    protected final String key;
    protected final String value;

    AbstractEnvVar(String key, String value) throws EnvVarsException {
        if (key==null) throw new EnvVarsException("Key cannot be null");
        if (value==null) throw new EnvVarsException("Value for "+key+" cannot be null");
        if (key.length()==0) throw new EnvVarsException("Key cannot be empty");
        if (key.contains("\"")) throw new EnvVarsException("Key cannot contain double-quotation character");

        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (!(o instanceof EnvVar)) return false;

        EnvVar other = (EnvVar) o;

        return key.equals(other.getKey());
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() { return value; }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo(EnvVar o) {
        return key.compareTo(o.getKey());
    }

    protected String getValueAsJSON() {
        return value.replace("\"", "\\\"");
    }
}
