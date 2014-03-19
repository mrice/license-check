package org.complykit.licensecheck.mojo;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author mrice
 */
class Utilities {

    /**
     * Construct the Maven-style coordinates from a given artifact
     *
     * @param artifact
     * @return
     */
    String coordinates(org.apache.maven.artifact.Artifact artifact) {
        if (artifact == null)
            return null;
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Since there doesn't seem to be a way to programmatically retrieve the parent object from the artifact (could
     * be wrong about this) and since I don't want to do any XML parsing or binding, this seems like a reasonable
     * approach -- if a little crude.
     * @param raw
     * @return
     */
    // TODO obviously this code needs a lot of error protection and handling
    String extractParentCoords(String raw) {
        if (raw == null)
            throw new NullPointerException("Attempted to extract parent coordinates from null pom contents");

        raw = raw.toLowerCase();

        String parentTagStart = "<parent>", parentTagStop = "</parent>";
        String groupTagStart = "<groupid>", groupTagStop = "</groupid>";
        String artifactTagStart = "<artifactid>", artifactTagStop = "</artifactid>";
        String versionTagStart = "<version>", versionTagStop = "</version>";

        if (!raw.contains(parentTagStart)) {
            return null;
        } else {
            String contents = null, group = null, artifact = null, version = null, coordinates = "";
            contents = raw.substring(raw.indexOf(parentTagStart) + parentTagStart.length(), raw.indexOf(parentTagStop));

            if (contents.contains(groupTagStart) && contents.contains(groupTagStop)) {
                group = contents.substring(contents.indexOf(groupTagStart) + groupTagStart.length(), contents.indexOf(groupTagStop));
                coordinates = group + ":";
            }

            if (contents.contains(artifactTagStart) && contents.contains(artifactTagStop)) {
                artifact = contents.substring(contents.indexOf(artifactTagStart) + artifactTagStart.length(), contents.indexOf(artifactTagStop));
                coordinates += artifact + ":";
            }

            if (contents.contains(versionTagStart) && contents.contains(versionTagStop)) {
                version = contents.substring(contents.indexOf(versionTagStart) + versionTagStart.length(), contents.indexOf(versionTagStop));
                coordinates += version;
            }

            return coordinates;
        }
    }

    /**
     * This is just a raw method for returning the contents of the file without using any dependencies (e.g., FileUtils).
     *
     * @param path
     * @return
     */
    String readPomContents(String path) {
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e1) {
            // TODO figure this one out
        }
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            // TODO figure this one out
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // TODO figure this one out
            }
        }
        return buffer.toString();
    }

    /**
     * This function looks for the license textual description. I didn't really want to parse the pom xml since we're
     * just looking for one little snippet of the content.
     *
     * @param raw the raw XML
     * @return
     */
    // TODO make this more elegant and less brittle
    String extractLicenseName(String raw) {
        if (raw == null)
            throw new NullPointerException("Attempted to extract license from null pom contents");

        raw = raw.toLowerCase();
        String licenseTagStart = "<license>", licenseTagStop = "</license>";
        String nameTagStart = "<name>", nameTagStop = "</name>";

        if (raw.indexOf(licenseTagStart) != -1) {
            String licenseContents = raw.substring(raw.indexOf(licenseTagStart) + licenseTagStart.length(), raw.indexOf(licenseTagStop));
            String name = licenseContents.substring(licenseContents.indexOf(nameTagStart) + nameTagStart.length(), licenseContents.indexOf(nameTagStop));
            return name;
        } else {
            return null;
        }
    }

    /**
     * This method looks for a resource file bundled with the plugin that contains a tab delimited version of the regex
     * and the codes. Future versions of the plugin will retrieve fresh versions of the file from the server so we can
     * take advantage of improvements in what we know and what we learn.
     */
    Set<LicenseMatcher> setupLicenseMatchers(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        BufferedReader reader = null;
        Set<LicenseMatcher> result = new HashSet<LicenseMatcher>();
        StringBuffer buffer = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } catch (Exception e) {
            //TODO figure out what to do with these
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
            }
        }

        String lines[] = buffer.toString().split("\n");
        for (String line : lines) {
            String columns[] = line.split("\\t");
            LicenseDescriptor descriptor = LicenseDescriptor.makeDescriptor(columns[0], columns[2], columns[3]);
            result.add(LicenseMatcher.make(descriptor));
        }
        return result;
    }

}
