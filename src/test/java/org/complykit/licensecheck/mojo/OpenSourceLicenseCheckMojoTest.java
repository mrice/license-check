package org.complykit.licensecheck.mojo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenSourceLicenseCheckMojoTest {


    @Test
    public void testConvertLicenseNameToCode() {
        OpenSourceLicenseCheckMojo mojo = new OpenSourceLicenseCheckMojo();
        assertEquals("apache-2.0", mojo.convertLicenseNameToCode("Apache License 2.0"));
        assertEquals("lgpl-2.1", mojo.convertLicenseNameToCode("GNU Lesser General Public License, Version 2.1"));
        assertEquals("lgpl-3.0", mojo.convertLicenseNameToCode("GNU Lesser General Public License, version 3"));
        assertEquals("gpl-1.0", mojo.convertLicenseNameToCode("GNU General Public License"));
        assertEquals("lgpl-2.0", mojo.convertLicenseNameToCode("Lesser General Public License (LGPL)"));
        assertEquals("epl-1.0", mojo.convertLicenseNameToCode("Eclipse Public License 1.0 (EPL-1.0)"));
        assertEquals("epl-1.0", mojo.convertLicenseNameToCode("Eclipse Public License 1.0"));
        assertEquals("mit", mojo.convertLicenseNameToCode("The MIT License"));
        assertEquals("w3c", mojo.convertLicenseNameToCode("The W3C SOFTWARE NOTICE AND LICENSE (W3C)"));
        assertEquals("cddl-1.0", mojo.convertLicenseNameToCode("Common Development and Distribution License 1.0"));
        assertEquals("apsl-2.0", mojo.convertLicenseNameToCode("Apple Public Source License 2.0"));
    }

}