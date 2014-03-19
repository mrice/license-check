package org.complykit.licensecheck.mojo;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author mrice
 */
public class MatchResultTest {

    @Test
    public void test() {

        MatchResult matchResult = MatchResult.makeResult(false, null);
        assertFalse(matchResult.isMatched());

        matchResult = MatchResult.makeResult(true, LicenseDescriptor.makeDescriptor("test", "test", "test"));
        assertTrue(matchResult.isMatched());
        assertNotNull(matchResult.getLicenseDescriptor());
        assertEquals(matchResult.getLicenseDescriptor().getCode(), "test");

    }

}
