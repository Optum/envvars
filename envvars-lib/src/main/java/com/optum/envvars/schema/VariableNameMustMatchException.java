package com.optum.envvars.schema;

public class VariableNameMustMatchException extends Exception {
    private final String variableName;
    private final EnvVarsVariableFilter offendingRule;

    public VariableNameMustMatchException(String variableName, EnvVarsVariableFilter offendingRule) {
        this.variableName = variableName;
        this.offendingRule = offendingRule;
    }

    public String getVariableName() {
        return variableName;
    }

    public EnvVarsVariableFilter getOffendingRule() {
        return offendingRule;
    }
}
