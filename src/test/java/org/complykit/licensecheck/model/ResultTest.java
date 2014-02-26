package org.complykit.licensecheck.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResultTest {

    @Test
    public void test() {
        String licenseDeclared = "licenseDeclared";
        String license = "license";
        String licenseTitle = "licenseTitle";
        String osiLink = "osiLink";

        Result result = Result.makeResult(licenseDeclared, license, licenseTitle, osiLink);
        assertNotNull(result.getLicensedDeclared());
        assertNotNull(result.getLicense());
        assertNotNull(result.getLicenseTitle());
        assertNotNull(result.getOsiLink());

        assertEquals(result.getLicensedDeclared(), licenseDeclared);
        assertEquals(result.getLicense(), license);
        assertEquals(result.getLicenseTitle(), licenseTitle);
        assertEquals(result.getOsiLink(), osiLink);

    }

}
