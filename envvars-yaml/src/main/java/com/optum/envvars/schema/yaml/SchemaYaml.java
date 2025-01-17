package com.optum.envvars.schema.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.schema.EnvVarsDefinitions;
import com.optum.envvars.schema.EnvVarsStaticSchema;
import com.optum.envvars.schema.EnvVarsVariableFilter;
import com.optum.envvars.schema.EnvVarsVariableFilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchemaYaml {
    public List<String> definitions;
    public String node_type;
    public SchemaVariableFiltersYaml variable_filters;

    public EnvVarsStaticSchema build(EnvVarsDefinitions envVarsDefinitions) throws EnvVarsException {
        List<EnvVarsVariableFilter> allof = makeList(envVarsDefinitions, variable_filters.getAllOf());
        List<EnvVarsVariableFilter> oneof = makeList(envVarsDefinitions, variable_filters.getOneOf());
        List<EnvVarsVariableFilter> noneof = makeList(envVarsDefinitions, variable_filters.getNoneOf());
        final EnvVarsVariableFilters envVarsVariableFilters = new EnvVarsVariableFilters(allof, oneof, noneof);
        EnvVarsStaticSchema envVarsStaticSchema = new EnvVarsStaticSchema();
        envVarsStaticSchema.setEnvVarsVariableFilters(envVarsVariableFilters);
        return envVarsStaticSchema;
    }

    private List<EnvVarsVariableFilter> makeList(EnvVarsDefinitions envVarsDefinitions, List<String> variableFilterList) throws EnvVarsException {
        if (variableFilterList.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<EnvVarsVariableFilter> list = new ArrayList<>(variableFilterList.size());
            for(String variableFilterName : variableFilterList) {
                EnvVarsVariableFilter envVarsVariableFilter = envVarsDefinitions.getVariableFilter(variableFilterName);
                if (envVarsVariableFilter==null)
                    throw new EnvVarsException("Definition not found for variable filter: " + variableFilterName);
                list.add(envVarsVariableFilter);
            }
            return list;
        }
    }

}
