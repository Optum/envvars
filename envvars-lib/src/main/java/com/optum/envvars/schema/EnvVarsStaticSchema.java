package com.optum.envvars.schema;

/**
 * This class will eventually be the owner of the rootNodesList, as soon as the runtime selectors are factored out.
 * As of now, unfortunately, the Node objects aren't reusable, because they bake-in the selector.  It was a
 * short-sighted design that I'm paying for now.
 */
public class EnvVarsStaticSchema {
    private EnvVarsVariableFilters envVarsVariableFilters;

    public void setEnvVarsVariableFilters(EnvVarsVariableFilters envVarsVariableFilters) {
        this.envVarsVariableFilters = envVarsVariableFilters;
    }

    public EnvVarsVariableFilters getEnvVarsVariableFilters() {
        return envVarsVariableFilters;
    }

}
