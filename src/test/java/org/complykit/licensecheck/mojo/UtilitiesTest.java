package org.complykit.licensecheck.mojo;

import junit.framework.Assert;
import org.apache.maven.artifact.Artifact;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * @author mrice
 */
public class UtilitiesTest {

    @Test
    public void testCoordinateUtil() {
        Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getGroupId()).thenReturn("t1");
        Mockito.when(artifact.getArtifactId()).thenReturn("t2");
        Mockito.when(artifact.getVersion()).thenReturn("t3");
        assertEquals("t1:t2:t3", new Utilities().coordinates(artifact));
    }

    @Test
    public void testExtractParentCoords() {
        Utilities utilities = new Utilities();

        String withoutParent = new StringBuffer()
                .append("<GROUPID>g</groupid>")
                .append("<artifactid>a</artifactid>")
                .append("<version>v</version>").toString();

        String withParent = new StringBuffer()
                .append("<PARENT>")
                .append(withoutParent)
                .append("</parent>").toString();

        assertNull(utilities.extractParentCoords(withoutParent));
        assertEquals("g:a:v", utilities.extractParentCoords(withParent));

    }

    @Ignore @Test
    public void testReadPomContents() {
        String contents = new Utilities().readPomContents("./test-pom.txt");
        assertNotNull(contents);
        assertTrue(contents.length()>0);
    }

    @Test
    public void testSetupLicenseMatchers() {
        Utilities util = new Utilities();
        Set<LicenseMatcher> matchers = util.setupLicenseMatchers("/test-licenses.txt");
        assertEquals(matchers.size(), 1); //test-licenses.txt should be fixed at 1
    }
}
