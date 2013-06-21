package com.michaelrice.licensecheck.model;

public class Result {

    private String result;
    private String license;
    private String artifact;

    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result;
    }

    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }

    public String getArtifact() {
        return artifact;
    }
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

}
