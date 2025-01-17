package com.optum.envvars.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.optum.envvars.EnvVarsEngine;
import com.optum.envvars.EnvVarsException;
import com.optum.envvars.mapdata.EnvVarsMapDataReader;
import com.optum.envvars.mapdata.json.JsonEnvVarsMapDataReader;
import com.optum.envvars.mapdata.source.impl.ScriptableEnvVarMapDataSource;
import com.optum.envvars.mapdata.yaml.YamlEnvVarsMapDataReader;
import com.optum.envvars.mapdata.yaml.YamlLoadWrapper;
import org.yaml.snakeyaml.Yaml;

public class EnvVars {

    public static void main(String[] args) {
        EnvVarsEngine envVarEngine = new EnvVarsEngine();
        final String values = args[0];
        try {
            for(int i=1; i<args.length; i+=2) {
                String filename = args[i+1];
                EnvVarsMapDataReader envVarsMapDataReader;
                if (filename.endsWith(".json")) {
                    envVarsMapDataReader = JsonEnvVarsMapDataReader.fromFile(filename);
                } else if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
                    envVarsMapDataReader = new YamlEnvVarsMapDataReader(YamlLoadWrapper.fromFile(new Yaml(), filename));
                } else {
                    throw new EnvVarsException("Unable to determine file format of \"" + filename + "\" based on extension.  Supported: .json .yml .yaml");
                }
                envVarEngine.add(new ScriptableEnvVarMapDataSource(values, args[i], envVarsMapDataReader));
            }
            envVarEngine.process();
            System.out.println(envVarEngine.toJSON());
        } catch (EnvVarsException e) {
            System.out.println(e.getMessage());
            Throwable cause = e.getCause();
            if (cause!=null) {
                System.out.println(cause.getMessage());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
