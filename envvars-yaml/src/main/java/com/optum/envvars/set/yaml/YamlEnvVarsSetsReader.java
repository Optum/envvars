package com.optum.envvars.set.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.yaml.YamlLoadWrapper;
import com.optum.envvars.set.EnvVarsSetsReader;
import com.optum.envvars.set.EnvVarsStaticSets;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class YamlEnvVarsSetsReader implements EnvVarsSetsReader {
    private final Yaml yaml;

    public YamlEnvVarsSetsReader() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        final Constructor constructor = new Constructor(SetsYaml.class, loaderOptions);
        this.yaml = new Yaml(constructor);
    }

    @Override
    public EnvVarsStaticSets buildFromString(String schemaString, String definitionsString) throws EnvVarsException {
        return null;
    }

    @Override
    public EnvVarsStaticSets buildFromResource(String yamlFilename) throws EnvVarsException {
        final YamlLoadWrapper<SetsYaml> yamlLoadWrapper = YamlLoadWrapper.fromResource(yaml, SetsYaml.class.getClassLoader(), yamlFilename);
        final SetsYaml setsYaml = yamlLoadWrapper.load();
        return setsYaml.build();
    }

}
