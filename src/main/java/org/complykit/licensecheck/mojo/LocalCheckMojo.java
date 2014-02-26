/*
The MIT License (MIT)

Copyright (c) 2013 Michael Rice <me@michaelrice.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.complykit.licensecheck.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.complykit.licensecheck.model.LicenseDescriptor;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * This plugin uses Aether to cruise through the dependencies in the project,
 * find the related pom files, extract the license information, and then convert
 * the licenses into codes. If a license cannot be verified or if the license
 * appears on the user's blacklist, then the build will fail.
 *
 * Original Github repo: https://github.com/mrice/license-check
 *
 * @author mrice
 * 
 */
@Mojo(name = "os-check")
public class LocalCheckMojo extends AbstractMojo {

    /**
     * This is the repo system required by Aether. For more about this and how Aether works in plugins, please visit
     * this site: http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     */
    @Component
    RepositorySystem        repoSystem;

    @Component
    MavenProject            project     = null;

    /**
     * The current repository and network configuration of Maven. For more about this, visit this site:
     * http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     *
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}")
    RepositorySystemSession repoSession;

    /**
     * This is the project's remote repositories that can be used for resolving plugins and their dependencies
     * 
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}")
    List<RemoteRepository> remoteRepos;

    /**
     * This is the maximum number of parents to search through (in case there's a malformed pom).
     */
    @Parameter(property = "os-check.recursion-limit", defaultValue = "12")
    int recursionLimit;

    /**
     * A list of artifacts that should be excluded from consideration. Example:
     * &lt;configuration&gt; &lt;excludes&gt;
     * &lt;param&gt;full:artifact:coords&lt;/param&gt;> &lt;/excludes&gt;
     * &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludes")
    String[] excludes;

    /**
     * A list of blacklisted licenses. Example: &lt;configuration&gt;
     * &lt;blacklist&gt; &lt;param&gt;agpl-3.0&lt;/param&gt;
     * &lt;param&gt;gpl-2.0&lt;/param&gt; &lt;param&gt;gpl-3.0&lt;/param&gt;
     * &lt;/blacklist&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.blacklist")
    String[] blacklist;

    /**
     * Used to hold the list of license descriptors. Generation is lazy on the
     * first method call to use it.
     * 
     */
    List<LicenseDescriptor> locallyStoredDescriptors = null;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("------------------------------------------------------------------------");
        getLog().info("   VALIDATING OPEN SOURCE LICENSES                                      ");
        getLog().info("------------------------------------------------------------------------");

        Set<org.apache.maven.artifact.Artifact> artifacts = project.getDependencyArtifacts();
        getLog().info("Validating licenses for " + artifacts.size() + " artifact(s)");

        Map<String, String> licenses = new HashMap<String, String>();

        boolean failBuild = false;
        for (org.apache.maven.artifact.Artifact mavenCoreArtifact : artifacts) {

            String coordinates = mavenCoreArtifact.getGroupId() + ":" + mavenCoreArtifact.getArtifactId() + ":" + mavenCoreArtifact.getVersion();

            if (!artifactIsOnExcludeList(coordinates)) {
                ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(new DefaultArtifact(coordinates));
                request.setRepositories(remoteRepos);

                ArtifactResult repoQueryResult = null;
                boolean artifactFound = false;
                try {
                    repoQueryResult = repoSystem.resolveArtifact(repoSession, request);
                    getLog().info(repoQueryResult.toString());
                    artifactFound = true;
                } catch (ArtifactResolutionException e) {
                    getLog().warn("caught an exception while trying to resolve "+coordinates, e);
                }

                if (artifactFound) {
                    String licenseName = recurseForLicenseName(repoQueryResult.getArtifact(), 0);
                    String code = convertLicenseNameToCode(licenseName);
                    if (code == null) {
                        failBuild = true;
                        code = "not found ("+coordinates+")";
                    } else if (licenseIsOnBlacklist(code)) {
                        failBuild = true;
                        code += " IS ON YOUR BLACKLIST";
                    }
                    licenses.put(mavenCoreArtifact.getArtifactId(), code);
                }
            }
            else {
                licenses.put(mavenCoreArtifact.getArtifactId(), "SKIPPED because artifact is on your exclude list");
            }

        }

        getLog().info("");
        getLog().info("This plugin validates that the artifacts you're using have a");
        getLog().info("license declared in the pom. It then tries to determine whether ");
        getLog().info("the license is one of the Open Source Initiative (OSI) approved ");
        getLog().info("licenses. If it can't find a match or if the license is on your ");
        getLog().info("declared blacklist, then the build will fail.");
        getLog().info("");
        getLog().info("This plugin and its author are not associated with the OSI.");
        getLog().info("Please send me feedback: me@michaelrice.com. Thanks!");
        getLog().info("");

        Set<String> keys = licenses.keySet();
        getLog().info("--[ Licenses found ]------ ");
        for (String artifact : keys) {
            getLog().info("\t" + artifact + ": " + licenses.get(artifact));
        }

        if (failBuild) {
            getLog().info("");
            getLog().info("RESULT: At least one license could not be verified or appears on your blacklist. Build fails.");
            getLog().info("Note that you can add coordinates to the ignore list if you want. See the instructions.");
            getLog().info("");
            throw new MojoFailureException("blacklist of unverifiable license");
        } else {
            getLog().info("");
            getLog().info("RESULT: license check complete, no issues found.");
            getLog().info("");
        }

    }

    /**
     * This is the primary method for searching for license names. We have to
     * recurse through parent poms because it's pretty common for developers to
     * declare a license in a parent project--not in the actual artifact for the
     * pom. This method has a safety valve for breaking the cursion in case,
     * somehow, we end up in a recursive loop.
     * 
     * @param aetherArtifact
     * @param recursionDepth
     * @return
     */
    String recurseForLicenseName(Artifact aetherArtifact, int recursionDepth) {
        if (aetherArtifact == null)
            throw new NullPointerException("attempted to recurse through null aether artifact");

        File artifactDirectory = aetherArtifact.getFile().getParentFile();
        String directoryPath = artifactDirectory.getAbsolutePath();
        directoryPath += "/" + aetherArtifact.getArtifactId() + "-" + aetherArtifact.getVersion() + ".pom";

        String pom = readPomContents(directoryPath);

        // first, look for a license
        String licenseName = extractLicenseName(pom);
        if (licenseName == null) {
            String parentArtifactCoords = extractParentCoords(pom);
            if (parentArtifactCoords != null) {
                // search for the artifact
                Artifact parent = retrieveAetherArtifact(parentArtifactCoords);
                if (parent != null) {
                    // check the recursion depth
                    if (recursionDepth >= recursionLimit)
                        return null; // TODO throw an exception
                    licenseName = recurseForLicenseName(parent, recursionDepth + 1);
                }
            }
        }
        return licenseName;
    }

    /**
     * This is just a raw method for returning the contents of the file without
     * using any dependencies (e.g., FileUtils).
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
                reader.close();
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
     * 
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
     * Uses Aether to retrieve an artifact from the repository.
     * 
     * @param coordinates as in groupId:artifactId:version
     * @return the located artifact
     */
    Artifact retrieveAetherArtifact(String coordinates) {

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(coordinates));
        request.setRepositories(remoteRepos);

        ArtifactResult aetherResult = null;
        try {
            aetherResult = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            getLog().error("Could not resolve parent artifact (" + coordinates + "): " + e.getMessage());
        }
        if (aetherResult != null)
            return aetherResult.getArtifact();
        else
            return null;
    }

    /**
     * This is the method that looks at the textual description of the license and returns a code version by running
     * regex. It needs a lot of optimization.
     * 
     * @param licenseName
     * @return
     */
    String convertLicenseNameToCode(String licenseName) {
        if (licenseName == null)
            return null;
        if (locallyStoredDescriptors == null) {
            loadDescriptors();
        }
        for (LicenseDescriptor descriptor : locallyStoredDescriptors) {
            // TODO there's gotta be a faster way to do this
            Pattern pattern = Pattern.compile(descriptor.getRegex(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(licenseName);
            if (matcher.find()) {
                return descriptor.getCode();
            }
        }
        return null;
    }

    /**
     * This method looks for a resource file bundled with the plugin that contains a tab delimited version of the regex
     * and the codes. Future versions of the plugin will retrieve fresh versions of the file from the server so we can
     * take advantage of improvements in what we know and what we learn.
     */
    void loadDescriptors() {
        String licensesPath = "/licenses.txt";
        InputStream is = getClass().getResourceAsStream(licensesPath);
        BufferedReader reader = null;
        locallyStoredDescriptors = new ArrayList<LicenseDescriptor>();
        StringBuffer buffer = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } catch (Exception e) {
            getLog().error(e);
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
            locallyStoredDescriptors.add(descriptor);
        }
    }

    /**
     * Just do a quick walk through of the excluded list and return true if match.
     * 
     * @param artifactKey the coordinates
     * @return true if it's on the exclude list, false if not or no exclude list was defined
     */
    boolean artifactIsOnExcludeList(String artifactKey) {
        if (excludes != null) {
            // walking the array should be easy since there shouldn't be more
            // than a handful on here
            for (int i = 0; i < excludes.length; i++) {
                if (artifactKey.toLowerCase().equals(excludes[i].toLowerCase()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Compare the license code to the user-provided black list
     * 
     * @param licenseCode the standardized code from complykit
     * @return true if the code is on the user-provided black list
     */
    boolean licenseIsOnBlacklist(String licenseCode) {
        if (blacklist != null) {
            // walking through the array should be small and fast for now
            for (int i = 0; i < blacklist.length; i++) {
                if (licenseCode.toLowerCase().equals(blacklist[i].toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

}
