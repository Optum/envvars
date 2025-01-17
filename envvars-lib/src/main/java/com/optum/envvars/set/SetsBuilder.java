package com.optum.envvars.set;

import java.util.List;
import java.util.Map;

public class SetsBuilder {
    public Map<String, List<String>> inject_sets;
    public Map<String, Map<String, String>> define_sets;

    public EnvVarsStaticSets build() {
        return new EnvVarsStaticSets(inject_sets, define_sets);
    }
}
