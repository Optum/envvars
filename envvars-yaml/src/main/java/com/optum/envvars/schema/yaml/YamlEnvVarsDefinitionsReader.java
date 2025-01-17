package com.optum.envvars.schema.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.yaml.YamlLoadWrapper;
import com.optum.envvars.schema.EnvVarsDefinitions;
import com.optum.envvars.schema.EnvVarsDefinitionsReader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.net.URL;

public class YamlEnvVarsDefinitionsReader implements EnvVarsDefinitionsReader {
    private final Yaml yaml;

    public YamlEnvVarsDefinitionsReader() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        final Constructor constructor = new Constructor(DefinitionsYaml.class, loaderOptions);
        this.yaml = new Yaml(constructor);
    }

    public EnvVarsDefinitions getFromString(String string) throws EnvVarsException {
        final YamlLoadWrapper<DefinitionsYaml> yamlLoadWrapper = YamlLoadWrapper.fromString(yaml, string);
        final DefinitionsYaml definitionsYaml = yamlLoadWrapper.load();
        return definitionsYaml.build();
    }

    @Override
    public EnvVarsDefinitions getFromURL(URL url) throws EnvVarsException {
        final YamlLoadWrapper<DefinitionsYaml> yamlLoadWrapper = YamlLoadWrapper.fromURL(yaml, url);
        final DefinitionsYaml definitionsYaml = yamlLoadWrapper.load();
        return definitionsYaml.build();
    }
}
