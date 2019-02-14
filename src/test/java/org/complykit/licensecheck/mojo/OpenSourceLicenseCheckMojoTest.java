package org.complykit.licensecheck.mojo;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

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

}