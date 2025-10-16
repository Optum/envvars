package com.optum.envvars.schema.yaml;

import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.yaml.YamlLoadWrapper;
import com.optum.envvars.schema.EnvVarsDefinitions;
import com.optum.envvars.schema.EnvVarsSchemaReader;
import com.optum.envvars.schema.EnvVarsStaticSchema;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.net.MalformedURLException;
import java.net.URL;

public class YamlEnvVarsSchemaReader implements EnvVarsSchemaReader {
    private final Yaml yaml;

    public YamlEnvVarsSchemaReader() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        final Constructor constructor = new Constructor(SchemaYaml.class, loaderOptions);
        this.yaml = new Yaml(constructor);
    }

    @Override
    public EnvVarsStaticSchema buildFromString(String schemaString, String definitionsString) throws EnvVarsException {
        final YamlLoadWrapper<SchemaYaml> yamlLoadWrapper = YamlLoadWrapper.fromString(yaml, schemaString);
        final SchemaYaml schemaYaml = yamlLoadWrapper.load();
        EnvVarsDefinitions envVarsDefinitions = new YamlEnvVarsDefinitionsReader().getFromString(definitionsString);
        return schemaYaml.build(envVarsDefinitions);
    }

    @Override
    public EnvVarsStaticSchema buildFromURL(URL url) throws EnvVarsException {
        final YamlLoadWrapper<SchemaYaml> yamlLoadWrapper = YamlLoadWrapper.fromURL(yaml, url);
        final SchemaYaml schemaYaml = yamlLoadWrapper.load();
        System.out.println("Schema Definitions for " + url + ": " + schemaYaml.definitions);

        EnvVarsDefinitions envVarsDefinitions = new EnvVarsDefinitions();
        for(String definition : schemaYaml.definitions) {
            try {
                URL definitionUrl = getClass().getClassLoader().getResource(definition);
                if (definitionUrl == null) {
                    definitionUrl = new URL(definition);
                }
                EnvVarsDefinitions definitionsFromURL = new YamlEnvVarsDefinitionsReader().getFromURL(definitionUrl);
                envVarsDefinitions.put(definitionsFromURL);
            } catch (MalformedURLException e) {
                throw new EnvVarsException("Bad URL in Schema Definition: " + definition);
            }

        }
        return schemaYaml.build(envVarsDefinitions);
    }
}
