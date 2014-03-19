package org.complykit.licensecheck.mojo;

import java.util.regex.Pattern;

/**
 * @author mrice
 */
class LicenseMatcher {
    private final LicenseDescriptor descriptor;
    private final Pattern pattern;

    private LicenseMatcher(final LicenseDescriptor descriptor) {
        this.descriptor = descriptor;
        this.pattern = Pattern.compile(descriptor.getRegex(), Pattern.CASE_INSENSITIVE);;
    }

    public static LicenseMatcher make(final LicenseDescriptor descriptor) {
        return new LicenseMatcher(descriptor);
    }

    MatchResult check(String test) {
        if (test == null)
            return null;    //TODO add guava to fail fast on this
        return MatchResult.makeResult(pattern.matcher(test).find(), this.descriptor);
    }

}
