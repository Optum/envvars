package com.optum.envvars.schema;

import com.optum.envvars.EnvVarsException;

import java.net.URL;

public interface EnvVarsSchemaReader {
    EnvVarsStaticSchema buildFromString(String schemaString, String definitionsString) throws EnvVarsException;
    EnvVarsStaticSchema buildFromURL(URL url) throws EnvVarsException;
}
