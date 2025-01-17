package com.optum.envvars;

/**
 * This is the END RESULT of all the envvars classes.
 */
public interface EnvVar {
    String getKey();
    String getValue();
    String asJSON();
    String asConfigMap();
}
