package com.optum.envvars.schema.yaml;

import java.util.Collections;
import java.util.List;

public class SchemaVariableFiltersYaml {
    public List<String> all_of;
    public List<String> one_of;
    public List<String> none_of;

    public List<String> getAllOf() {
        if (all_of==null)
            return Collections.emptyList();
        return all_of;
    }

    public List<String> getOneOf() {
        if (one_of==null)
            return Collections.emptyList();
        return one_of;
    }

    public List<String> getNoneOf() {
        if (none_of==null)
            return Collections.emptyList();
        return none_of;
    }
}
