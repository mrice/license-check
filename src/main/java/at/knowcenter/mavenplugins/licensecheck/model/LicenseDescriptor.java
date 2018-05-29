package at.knowcenter.mavenplugins.licensecheck.model;

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseDescriptor {
    private final String code;
    private final String licenseName;
    private final String regex;
    private final Pattern regexCompiled;

    public LicenseDescriptor(String code, String licenseName, String regex) {
        this.code = code;
        this.licenseName = licenseName;
        this.regex = regex;
        this.regexCompiled = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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

    public Matcher getMatcher(String text) {
        return regexCompiled.matcher(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicenseDescriptor that = (LicenseDescriptor) o;
        return Objects.equals(getCode(), that.getCode()) &&
                Objects.equals(getLicenseName(), that.getLicenseName()) &&
                Objects.equals(getRegex(), that.getRegex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getLicenseName(), getRegex());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("code", code)
                          .add("licenseName", licenseName)
                          .add("regex", regex)
                          .toString();
    }


}
