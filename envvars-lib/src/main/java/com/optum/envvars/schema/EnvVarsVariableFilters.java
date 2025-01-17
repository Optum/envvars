package com.optum.envvars.schema;

import java.util.List;

public class EnvVarsVariableFilters {
    private final List<EnvVarsVariableFilter> allof;
    private final List<EnvVarsVariableFilter> oneof;
    private final List<EnvVarsVariableFilter> noneof;

    public EnvVarsVariableFilters(List<EnvVarsVariableFilter> allof, List<EnvVarsVariableFilter> oneof, List<EnvVarsVariableFilter> noneof) {
        this.allof = allof;
        this.oneof = oneof;
        this.noneof = noneof;
    }

    public void confirmSuitability(String variableName) throws VariableNameMustNotMatchException, VariableNameNotOneOfException, VariableNameMustMatchException {
        // Noneof failures are fast, so process them first.
        for(EnvVarsVariableFilter mustnot : noneof) {
            if (mustnot.getRegex().matcher(variableName).matches()) {
                throw new VariableNameMustNotMatchException(variableName, mustnot);
            }
        }

        // Allof failures are fast, so process them second.
        for(EnvVarsVariableFilter must : allof) {
            if (!must.getRegex().matcher(variableName).matches()) {
                throw new VariableNameMustMatchException(variableName, must);
            }
        }

        // If this is empty, we are allowed by default.
        if (oneof.isEmpty())
            return;

        // Once we know we aren't forbidden, allows are success-fast, so process them third.
        for(EnvVarsVariableFilter may : oneof) {
            if (may.getRegex().matcher(variableName).matches()) {
                return;
            }
        }

        // If we must match at least one may from oneof.
        throw new VariableNameNotOneOfException(variableName, oneof);
    }

}
