package org.depwatch.licensecheck.model;

public class Result {

    private String licenseDeclared;
    private String license;

    public String getLicensedDeclared() {
        return licenseDeclared;
    }
    public void setLicenseDeclared(String licenseDeclared) {
        this.licenseDeclared = licenseDeclared;
    }

    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }


}
