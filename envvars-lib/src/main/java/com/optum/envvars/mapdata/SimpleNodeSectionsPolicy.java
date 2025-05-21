package com.optum.envvars.mapdata;

public class SimpleNodeSectionsPolicy implements NodeSectionsPolicy {
    private final boolean declareAllowed;
    private final boolean defineAllowed;
    private final boolean injectAllowed;
    private final boolean remapAllowed;
    private final boolean skipInjectIfNotDefinedAllowed;
    private final boolean defineSecretsAllowed;
    private final boolean defineReferencesAllowed;

    public SimpleNodeSectionsPolicy(boolean declareAllowed, boolean defineAllowed, boolean injectAllowed, boolean remapAllowed, boolean skipInjectIfNotDefinedAllowed, boolean defineSecretsAllowed, boolean defineReferencesAllowed) {
        this.declareAllowed = declareAllowed;
        this.defineAllowed = defineAllowed;
        this.injectAllowed = injectAllowed;
        this.remapAllowed = remapAllowed;
        this.skipInjectIfNotDefinedAllowed = skipInjectIfNotDefinedAllowed;
        this.defineSecretsAllowed = defineSecretsAllowed;
        this.defineReferencesAllowed = defineReferencesAllowed;
    }

    public boolean isDeclareAllowed() {
        return declareAllowed;
    }

    public boolean isDefineAllowed() {
        return defineAllowed;
    }

    public boolean isInjectAllowed() {
        return injectAllowed;
    }

    public boolean isRemapAllowed()
    {
        return remapAllowed;
    }

    public boolean isSkipInjectIfNotDefinedAllowed() {
        return skipInjectIfNotDefinedAllowed;
    }

    public boolean isDefineSecretsAllowed() {
        return defineSecretsAllowed;
    }

    public boolean isDefineReferencesAllowed() {
        return defineReferencesAllowed;
    };

}
