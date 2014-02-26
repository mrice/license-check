package org.complykit.licensecheck.model;

public class LicenseDescriptor {

    private final String code;
    private final String licenseName;
    private final String regex;

    private LicenseDescriptor() {
        this.code = null;
        this.licenseName = null;
        this.regex = null;
    }

    private LicenseDescriptor(String code, String licenseName, String regex) {
        this.code = code;
        this.licenseName = licenseName;
        this.regex = regex;
    }

    public static LicenseDescriptor makeDescriptor(String code, String licenseName, String regex) {
        return new LicenseDescriptor(code, licenseName, regex);
    }

    public String getCode() {
        return code;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getRegex() {
        return regex;
    }

}
