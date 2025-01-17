package com.optum.envvars.schema;

import java.util.regex.Pattern;

public class EnvVarsVariableFilter {
    /**
     * We store a compiled Pattern so it only has to be compiled once.
     */
    private final Pattern regexPattern;
    private final String name;
    private final String description;
    private final String usage;

    public EnvVarsVariableFilter(String name, String regex, String description, String usage) {
        this.name = name;
        this.regexPattern = Pattern.compile(regex);
        this.description = description;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public Pattern getRegex() {
        return regexPattern;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }
}
