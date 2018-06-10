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

import org.complykit.licensecheck.model.LicenseDescriptor;
import org.complykit.licensecheck.model.MaximumRecursionDepthReachedException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.complykit.licensecheck.model.LicenseDescriptor;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.inject.InjectionException;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.google.common.io.Files.newReader;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This plugin uses Aether to cruise through the dependencies in the project, find the related pom files,
 * extract the license information, and then convert the licenses into codes.
 * If a license cannot be verified or if the license appears on the user's blacklist, then the build will fail.
 * <p>
 * For more, visit https://github.com/mrice/license-check
 *
 * @author michael.rice
 */
@Mojo(name = "os-check", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class OpenSourceLicenseCheckMojo extends AbstractMojo {

    private static final int LICENSE_CODE_COLUMN = 0;
    private static final int LICENSE_NAME_COLUMN = 2;
    private static final int LICENSE_REGEX_COLUMN = 3;

    private static final Locale LOCALE = Locale.ENGLISH;

    @Component
    private MavenProject project;
    /**
     * This is the repo system required by Aether
     * <p>
     * For more, visit http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     */
    @Component
    private
    RepositorySystem repoSystem;
    /**
     * The current repository and network configuration of Maven
     * <p>
     * For more, visit http://blog.sonatype.com/people/2011/01/how-to-use-aether-in-maven-plugins/
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private
    RepositorySystemSession repoSession;

    /**
     * This is the project's remote repositories that can be used for resolving plugins and their dependencies
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private
    List<RemoteRepository> remoteRepos;

    /**
     * This is the maximum number of parents to search through (in case there's a malformed pom).
     */
    @Parameter(property = "os-check.recursion-limit", defaultValue = "12")
    private
    int maxSearchDepth;

    /**
     * A list of artifacts that should be excluded from consideration. Example: &lt;configuration&gt; &lt;excludes&gt;
     * &lt;param&gt;full:artifact:coords&lt;/param&gt;>
     * &lt;/excludes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludes")
    private
    String[] excludes;

    /**
     * A list of artifacts that should be excluded from consideration. Example: &lt;configuration&gt;
     * &lt;excludesRegex&gt; &lt;param&gt;full:artifact:coords&lt;/param&gt;>
     * &lt;/excludesRegex&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludesRegex")
    private
    String[] excludesRegex;

    @Parameter(property = "os-check.excludesNoLicense")
    private
    boolean excludeNoLicense;

    /**
     * A list of blacklisted licenses. Example: &lt;configuration&gt; &lt;blacklist&gt;
     * &lt;param&gt;agpl-3.0&lt;/param&gt; &lt;param&gt;gpl-2.0&lt;/param&gt;
     * &lt;param&gt;gpl-3.0&lt;/param&gt; &lt;/blacklist&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.blacklist")
    private
    String[] blacklist;

    /**
     * A list of whitelisted licenses. Example: &lt;configuration&gt; &lt;whitelist&gt;
     * &lt;param&gt;agpl-3.0&lt;/param&gt; &lt;param&gt;gpl-2.0&lt;/param&gt;
     * &lt;param&gt;gpl-3.0&lt;/param&gt; &lt;/blacklist&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.whitelist")
    private
    String[] whitelist;

    /**
     * A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
     * Example: &lt;configuration&gt; &lt;excludedScopes&gt; &lt;param&gt;test&lt;/param&gt;
     * &lt;param&gt;provided&lt;/param&gt; &lt;/excludedScopes&gt; &lt;/configuration&gt;
     */
    @Parameter(property = "os-check.excludedScopes")
    private
    String[] excludedScopes;

    /**
     * Used to hold the list of license descriptors. Generation is lazy on the first method call to use it.
     */
    private List<LicenseDescriptor> descriptors;

    private static String lowerCaseString(String s) {
        return s.toLowerCase(LOCALE);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("------------------------------------------------------------------------");
        getLog().info("VALIDATING OPEN SOURCE LICENSES                                         ");
        getLog().info("------------------------------------------------------------------------");

        final Set<String> excludeSet = getAsLowerCaseSetWithoutNull(excludes);
        final Set<String> blacklistSet = getAsLowerCaseSetWithoutNull(blacklist);
        final Set<String> whitelistSet = getAsLowerCaseSetWithoutNull(whitelist);
        final Set<String> excludedScopesSet = getAsLowerCaseSetWithoutNull(excludedScopes);
        final Set<Pattern> excludePatternSet = getAsPatternSet(excludesRegex);

        if (project == null) {
            throw new InjectionException("Maven Project not injected into Plugin!");
        }

        final Set<Artifact> artifacts = project.getArtifacts();
        artifacts.addAll(project.getDependencyArtifacts());
        getLog().info("Validating licenses for " + artifacts.size() + " artifact(s)");

    final Map<Artifact, String> licenses = new HashMap<Artifact, String>();

        boolean buildFails = false;
        for (final Artifact artifact : artifacts) {
            if (!artifactIsOnExcludeList(excludeSet, excludePatternSet, excludedScopesSet, artifact)) {
                final ArtifactRequest request = new ArtifactRequest();
                request.setArtifact(RepositoryUtils.toArtifact(artifact));
                request.setRepositories(remoteRepos);

                ArtifactResult result = null;
                try {
                    result = repoSystem.resolveArtifact(repoSession, request);
                    getLog().info(result.toString());
                } catch (@NotNull final ArtifactResolutionException e) {
                    getLog().error("Error during Artifact Resolution", e);
                }

                String licenseName = "";
                try {
                    if (result != null) {
                        licenseName = recurseForLicenseName(RepositoryUtils.toArtifact(result.getArtifact()), 0);
                    }
                } catch (IOException e) {
                    getLog().error("Error reading license information", e);
                }
                String code = convertLicenseNameToCode(licenseName);
                if (code == null) {
                    if (!excludeNoLicense) {
                        buildFails = true;
                        code = "[NULL] LICENSE '" + licenseName + "' IS UNKNOWN";
                        getLog().warn("Build will fail because of artifact '" + toCoordinates(
                                artifact) + "' and license'" + licenseName + "'.");
                    }
                } else if (!blacklistSet.isEmpty() && isContained(blacklistSet, code)) {
                    buildFails = true;
                    code += " IS ON YOUR BLACKLIST";
                } else if (!whitelistSet.isEmpty() && !isContained(whitelistSet, code)) {
                    buildFails = true;
                    code += " IS NOT ON YOUR WHITELIST";
                }
                licenses.put(artifact.getArtifactId(), code);
            } else {
                licenses.put(artifact.getArtifactId(), "SKIPPED because artifact is on your exclude list");
            }
        String licenseName = "";
        try {
          licenseName = recurseForLicenseName(RepositoryUtils.toArtifact(result.getArtifact()), 0);
        } catch (IOException e) {
          getLog().error("Error reading license information", e);
        } catch (XmlPullParserException e) {
          getLog().error("Error parsing maven model", e);
        }
        String code = convertLicenseNameToCode(licenseName);
        if (code == null || code.equals("")) {
          if (excludeNoLicense==false) {
            buildFails = true;
            getLog().warn("Build will fail because of artifact '" + toCoordinates(artifact) + "' and license'" + licenseName + "'.");
          }
        } else if (blacklistSet.isEmpty() == false && isContained(blacklistSet, code)) {
          buildFails = true;
          code += " IS ON YOUR BLACKLIST";
        } else if (whitelistSet.isEmpty() == false && isContained(whitelistSet, code) == false) {
          buildFails = true;
          code += " IS NOT ON YOUR WHITELIST";
        }
        licenses.put(artifact, code);
      } else {
        licenses.put(artifact, "SKIPPED because artifact is on your exclude list");
      }

        }

        getLog().info("");
        getLog().info("This plugin validates that the artifacts you're using have a");
        getLog().info("known license declared in the pom.");
        getLog().info("If it can't find a match or if the license is on your");
        getLog().info("declared blacklist or not on your declared whitelist, then the build will fail.");
        getLog().info("");
        getLog().info("--[ Licenses found ]------ ");
        for (final Map.Entry<String, String> artifact : licenses.entrySet()) {
            getLog().info("\t" + artifact.getKey() + ": " + artifact.getValue());
        }
    getLog().info("");
    getLog().info("This plugin validates that the artifacts you're using have a");
    getLog().info("license declared in the pom. It then tries to determine whether ");
    getLog().info("the license is one of the Open Source Initiative (OSI) approved ");
    getLog().info("licenses. If it can't find a match or if the license is on your ");
    getLog().info("declared blacklist or not on your declared whitelist, then the build will fail.");
    getLog().info("");
    getLog().info("This plugin and its author are not associated with the OSI.");
    getLog().info("Please send me feedback: me@michaelrice.com. Thanks!");
    getLog().info("");
    final Set<Artifact> keys = licenses.keySet();
    getLog().info("--[ Licenses found ]------ ");
    for (final Artifact artifact : keys) {
      getLog().info("\t" + artifact.getArtifactId() + ":" +artifact.getVersion()+ ": " + licenses.get(artifact));
    }

        if (buildFails) {
            getLog().warn("");
            getLog().warn(
                    "RESULT: At least one license could not be verified or appears on your blacklist or is not on your whitelist. Build fails.");
            getLog().warn("");
            throw new MojoFailureException("blacklist/whitelist of unverifiable license");
        }
        getLog().info("");
        getLog().info("RESULT: license check complete, no issues found.");
        getLog().info("");

    }

    @NotNull Set<String> getAsLowerCaseSetWithoutNull(@Nullable final String[] src) {
        return Arrays.stream(src != null ? src : new String[0])
                     .filter(Objects::nonNull)
                     .map(OpenSourceLicenseCheckMojo::lowerCaseString)
                     .collect(Collectors.toSet());
    }

    @Nullable
    private Pattern compilePattern(@NotNull String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (@NotNull final PatternSyntaxException e) {
            getLog().warn("The regex " + pattern + " is invalid: ", e);
        }
        return null;

    }

    @NotNull
    private Set<Pattern> getAsPatternSet(@Nullable final String[] src) {
        return Arrays.stream(src != null ? src : new String[0]).unordered()
                     .filter(Objects::nonNull)
                     .distinct()
                     .map(this::compilePattern)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toSet());
    }

    @NotNull
    private String toCoordinates(@NotNull final Artifact artifact) {
        return String.format("%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    @Nullable
    private String recurseForLicenseName(@NotNull final Artifact artifact,
                                         final int currentDepth) throws IOException, MojoFailureException {
  String recurseForLicenseName(final Artifact artifact, final int currentDepth) throws IOException, XmlPullParserException {

        final String pom = readPomContents(artifact);
    final File artifactDirectory = artifact.getFile().getParentFile();
    File pomFile = new File(artifactDirectory.getAbsolutePath(), artifact.getArtifactId() + "-" + artifact.getVersion() + ".pom");

    final Model model = readPomContents(pomFile);
    if(model == null){
      return null;
    }

        // first, look for a license
        String licenseName = extractLicenseName(pom);
        if (licenseName == null) {
            final String parentArtifactCoords = extractParentCoords(pom);
            if (parentArtifactCoords != null) {
                // search for the artifact
                final Artifact parent = retrieveArtifact(parentArtifactCoords);
                if (parent != null) {
                    // check the recursion depth
                    if (currentDepth >= maxSearchDepth) {
                        throw new MaximumRecursionDepthReachedException(String.format(
                                "The maximum recursion depth of %d has been reached when handling transitive dependencies. Fix your project or extend it.",
                                maxSearchDepth));
                    }
                    licenseName = recurseForLicenseName(parent, currentDepth + 1);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return licenseName;
    }
    // first, look for a license
    String licenseName = extractLicenseName(model);
    if (licenseName == null || licenseName.equalsIgnoreCase("")) {
      final Parent parentArtifactCoords = model.getParent();
      if (parentArtifactCoords != null) {
        // search for the artifact
        final Artifact parent = retrieveArtifact(parentArtifactCoords);
        if (parent != null) {
          // check the recursion depth
          if (currentDepth >= maxSearchDepth) {
            return null; // TODO throw an exception
          }
          licenseName = recurseForLicenseName(parent, currentDepth + 1);
        } else {
          return null;
        }
      } else {
        return null;
      }
    }
    return licenseName;
  }

    @NotNull
    private String readPomContents(@NotNull final Artifact artifact) throws IOException, MojoFailureException {
        FileSystem jarFs = null;
        try {

            final File artifactDirectory = artifact.getFile().getParentFile();
            String path = artifactDirectory.getAbsolutePath();
            path += "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".pom";


            final StringBuilder buffer = new StringBuilder();
            BufferedReader reader = null;

            //check if the file exists
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                getLog().debug(String.format("File %s not found!", pathFile));
                //read the pom from the jar
                getLog().debug(String.format("File %s will be used instead!", artifact.getFile()));
                if (artifact.getFile().exists()) {
                    getLog().debug(String.format("File %s exists!", artifact.getFile()));
                    if (artifact.getFile()
                                .toString()
                                .toLowerCase()
                                .endsWith("pom.xml")) { // if the artifact itself is a pom, load it
                        getLog().debug(String.format("File %s is a pom, using that!", artifact.getFile()));
                        reader = newReader(artifact.getFile(), UTF_8);
                    } else if (artifact.getFile()
                                       .toString()
                                       .toLowerCase()
                                       .endsWith(".jar")) {
                        getLog().debug(
                                String.format("File %s is a jar, trying to find pom inside!",
                                              artifact.getFile()));
                        // if the artifact is a jar, try to open the pom inside
                        jarFs = FileSystems.newFileSystem(artifact.getFile().toPath(), null);
                        Path jarPom = jarFs.getPath(
                                String.format("META-INF/maven/%s/%s/pom.xml", artifact.getGroupId(),
                                              artifact.getArtifactId()));
                        if (Files.exists(jarPom)) {
                            getLog().debug(String.format("File %s from inside jar will be used!", jarPom));
                            reader = Files.newBufferedReader(jarPom);
                        } else {
                            getLog().debug(String.format("File %s not found!", jarPom));
                        }

                    }
                }

            } else {
                getLog().debug(String.format("File %s will be used as pom!", path));
                reader = newReader(pathFile, UTF_8);
            }
            if (reader == null) {
                throw new MojoFailureException(String.format("No pom file for artifact %s found!", artifact.getFile()));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }


            reader.close();


            return buffer.toString();
        } finally {
            if (jarFs != null) {
                jarFs.close();
            }
        }
    }
  Model readPomContents(final File pomFile) throws IOException, XmlPullParserException {
    if(!pomFile.exists()){
      return null;
    }
    MavenXpp3Reader modelReader = new MavenXpp3Reader();
    BufferedReader reader = new BufferedReader(new FileReader(pomFile));
    Model model = modelReader.read(reader);
    reader.close();
    return model;
  }

  /**
   * This function looks for the license textual description. I didn't really want to parse the pom xml since we're
   * just looking for one little snippet of the content.
   *
   * @param model
   * @return
   */
  // TODO make this more elegant and less brittle
  String extractLicenseName(final Model model)
  {
    List<License> licenses = model.getLicenses();
    if(licenses == null || licenses.isEmpty()){
      return null;
    }
    else return licenses.get(0).getName();
  }
    /**
     * This function looks for the license textual description. I didn't really want to parse the pom xml since we're
     * just looking for one little snippet of the content.
     *
     * @param raw
     * @return
     */
    // TODO make this more elegant and less brittle
    @Nullable
    private String extractLicenseName(@NotNull final String raw) {
        final String licenseTagStart = "<license>", licenseTagStop = "</license>";
        final String nameTagStart = "<name>", nameTagStop = "</name>";
        if (raw.contains(licenseTagStart)) {
            final String licenseContents = raw.substring(raw.indexOf(licenseTagStart) + licenseTagStart.length(),
                                                         raw.indexOf(licenseTagStop));
            return licenseContents.substring(licenseContents.indexOf(nameTagStart) + nameTagStart.length(),
                                             licenseContents.indexOf(nameTagStop));
        }
        return null;
    }

    /**
     * @param raw
     * @return
     */
    // TODO obviously this code needs a lot of error protection and handling
    @Nullable
    private String extractParentCoords(@NotNull final String raw) {
        final String parentTagStart = "<parent>", parentTagStop = "</parent>";
        final String groupTagStart = "<groupId>", groupTagStop = "</groupId>";
        final String artifactTagStart = "<artifactId>", artifactTagStop = "</artifactId>";
        final String versionTagStart = "<version>", versionTagStop = "</version>";

        if (!raw.contains(parentTagStart)) {
            return null;
        }
        final String contents = raw.substring(raw.indexOf(parentTagStart) + parentTagStart.length(),
                                              raw.indexOf(parentTagStop));
        final String group = contents.substring(contents.indexOf(groupTagStart) + groupTagStart.length(),
                                                contents.indexOf(groupTagStop));
        final String artifact = contents.substring(contents.indexOf(artifactTagStart) + artifactTagStart.length(),
                                                   contents.indexOf(artifactTagStop));
        final String version = contents.substring(contents.indexOf(versionTagStart) + versionTagStart.length(),
                                                  contents.indexOf(versionTagStop));
        return group + ":" + artifact + ":" + version;
    }

  /**
   * Uses Aether to retrieve an artifact from the repository.
   *
   * @param parent
   * @return the located artifact
   */
  Artifact retrieveArtifact(final Parent parent)
  {
    final ArtifactRequest request = new ArtifactRequest();
    request.setArtifact(new DefaultArtifact(parent.getGroupId(),parent.getArtifactId(),"pom",parent.getVersion()));
    request.setRepositories(remoteRepos);
    /**
     * Uses Aether to retrieve an artifact from the repository.
     *
     * @param coordinates as in groupId:artifactId:version
     * @return the located artifact
     */
    @Nullable
    private Artifact retrieveArtifact(final String coordinates) {

        final ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(coordinates));
        request.setRepositories(remoteRepos);

    ArtifactResult result = null;
    try {
      result = repoSystem.resolveArtifact(repoSession, request);
    } catch (final ArtifactResolutionException e) {
      getLog().error("Could not resolve parent artifact (" + parent.getId() + "): " + e.getMessage());
    }
        ArtifactResult result = null;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (@NotNull final ArtifactResolutionException e) {
            try {
                getLog().debug("Artifact jar not found, trying to find pom instead", e);
                //if the artifact jar is not found, retry the request for the pom instead of the jar
                org.eclipse.aether.artifact.Artifact art = request.getArtifact();
                request
                        .setArtifact(
                                new DefaultArtifact(art.getGroupId(), art.getArtifactId(), "pom", art.getVersion()));
                result = repoSystem.resolveArtifact(repoSession, request);
            } catch (@NotNull final ArtifactResolutionException ex) {
                getLog().error(
                        String.format("Could not resolve parent artifact (%s): ", coordinates), ex);
            }
        }

        if (result != null) {
            return RepositoryUtils.toArtifact(result.getArtifact());
        }
        return null;
    }

    /**
     * This is the method that looks at the textual description of the license and returns a code version by running
     * regex. It needs a lot of optimization.
     *
     * @param licenseName
     * @return
     */
    @Nullable String convertLicenseNameToCode(@Nullable final String licenseName) {
        if (licenseName == null) {
            return null;
        }
        if (descriptors == null) {
            loadDescriptors();
        }
        return descriptors.stream()
                          .sequential()
                          .filter(licenseDescriptor -> licenseDescriptor.getMatcher(licenseName).find())
                          .findFirst()
                          .map(LicenseDescriptor::getCode)
                          .orElse(null);
    }

    /**
     * This method looks for a resource file bundled with the plugin that contains a tab delimited version of the regex
     * and the codes. Future versions of the plugin may retrieve fresh versions of the file from the server so we can
     * take advantage of improvements in what we know and what we learn.
     */
    // TODO I know, I know... this is really raw... will make it prettier
    private void loadDescriptors() {

        final String licensesPath = "/licenses.txt";
        final InputStream is = getClass().getResourceAsStream(licensesPath);
        descriptors = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        } catch (@NotNull final Exception e) {
            getLog().error(e);
        }

        final String[] lines = buffer.toString().split("\n");
        for (final String line : lines) {
            final String[] columns = line.split("\\t");
            final LicenseDescriptor descriptor = new LicenseDescriptor(columns[LICENSE_CODE_COLUMN],
                                                                       columns[LICENSE_NAME_COLUMN],
                                                                       columns[LICENSE_REGEX_COLUMN]);
            descriptors.add(descriptor);
        }
    }

    /**
     * Checks to see if an artifact is on the user's exclude list
     *
     * @param excludeSet
     * @param patternSet
     * @param excludedScopes
     * @param artifact
     * @return
     */
    private boolean artifactIsOnExcludeList(final Set<String> excludeSet, @NotNull final Set<Pattern> patternSet,
                                            @NotNull final Set<String> excludedScopes,
                                            @NotNull final Artifact artifact) {
        return excludedScopes.contains(artifact.getScope()) || isExcludedTemplate(excludeSet, patternSet,
                                                                                  toCoordinates(artifact));
    }

    private boolean isExcludedTemplate(final Set<String> excludeSet, @NotNull final Set<Pattern> patternSet,
                                       @NotNull final String template) {
        if (isContained(excludeSet, template)) {
            return true;
        }
        for (final Pattern pattern : patternSet) {
            if (pattern.matcher(template).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isContained(@Nullable final Set<String> set, @Nullable final String template) {
        return set != null && template != null && set.contains(lowerCaseString(template));
    }

}
