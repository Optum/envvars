package com.optum.envvars.schema;

import com.optum.envvars.EnvVarsException;

import java.net.URL;

public interface EnvVarsDefinitionsReader {
    EnvVarsDefinitions getFromString(String string) throws EnvVarsException;
    EnvVarsDefinitions getFromURL(URL url) throws EnvVarsException;
}
