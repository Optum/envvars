package com.optum.envvars.schema;

import java.util.List;

public class VariableNameNotOneOfException extends Exception {
    private final String variableName;
    private final List<EnvVarsVariableFilter> oneof;

    public VariableNameNotOneOfException(String variableName, List<EnvVarsVariableFilter> oneof) {
        this.variableName = variableName;
        this.oneof = oneof;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<EnvVarsVariableFilter> getOneOf() {
        return oneof;
    }
}
