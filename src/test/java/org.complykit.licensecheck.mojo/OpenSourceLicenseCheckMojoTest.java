package org.complykit.licensecheck.mojo;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OpenSourceLicenseCheckMojoTest {

    @Test
    public void testGetAsLowerCaseSet() {
        String src[] = {"Test1", "Test2", "Test3"};
        Set<String> expected = new HashSet<String>();
        expected.add("test1");
        expected.add("test2");
        expected.add("test3");

        Set<String> result = new OpenSourceLicenseCheckMojo().getAsLowerCaseSet(src);

        assertTrue(result.containsAll(expected));
    }

    @Test
    public void testConvertLicenseNameToCode(){
        OpenSourceLicenseCheckMojo mojo = new OpenSourceLicenseCheckMojo();
        assertEquals("apache-2.0",mojo.convertLicenseNameToCode("Apache License 2.0"));
    }

}