package org.complykit.licensecheck.model;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * This is a very trivial test
 * @author mrice
 *
 */
public class LicenseDescriptorTest {

    @Test
    public void testMakeDescriptor() {

        String testCode = "testcode";
        String testLicenseName = "testlicensename";
        String testRegEx = "testRegEx";

        LicenseDescriptor descriptor = LicenseDescriptor.makeDescriptor(testCode, testLicenseName, testRegEx);
        assertNotNull(descriptor.getCode());
        assertNotNull(descriptor.getLicenseName());
        assertNotNull(descriptor.getRegex());

        assertEquals(descriptor.getCode(), testCode);
        assertEquals(descriptor.getLicenseName(), testLicenseName);
        assertEquals(descriptor.getRegex(), testRegEx);

    }

}
