package org.complykit.licensecheck.mojo;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author mrice
 */
public class LicenseMatcherTest {

    @Test
    public void testHighLevel() {
        LicenseMatcher matcher = new Utilities().setupLicenseMatchers("/test-licenses.txt").iterator().next();
        assertTrue(matcher.check("Academic Free 3").isMatched());
        assertNotNull(matcher.check("Academic Free 3").getLicenseDescriptor());
    }

}
