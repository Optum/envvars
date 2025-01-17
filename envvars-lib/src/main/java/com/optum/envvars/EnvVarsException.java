package com.optum.envvars;

public class EnvVarsException extends Exception {

    public EnvVarsException(String message) {
        super(message);
    }

    public EnvVarsException(String message, Throwable t) {
        super(message, t);
    }

}
