package com.optum.envvars.mapdata;

public interface NodeSectionsPolicy {
    boolean isDeclareAllowed();
    boolean isDefineAllowed();
    boolean isInjectAllowed();
    boolean isRemapAllowed();
    boolean isSkipInjectIfNotDefinedAllowed();
    boolean isDefineSecretsAllowed();
    boolean isDefineConfigMapsAllowed();
}
