package com.optum.envvars.schema;

public class VariableNameMustNotMatchException extends Exception {
    private final String variableName;
    private final EnvVarsVariableFilter offendingRule;

    public VariableNameMustNotMatchException(String variableName, EnvVarsVariableFilter offendingRule) {
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
