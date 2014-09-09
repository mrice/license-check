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

import java.util.Iterator;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.complykit.licensecheck.model.Result;

import com.google.gson.Gson;

/**
 * This early version of the license check plug in simply checks to see whether there is a license
 * declared for a given dependency. This is important because, technically, if no license is 
 * associated with a given dependency, that could mean that the owner of the dependency retains
 * all rights associated with the library. Future versions of the license-check will go further.
 * 
 * @author michael rice
 *
 */
@Deprecated
@Mojo(name="check")
public class ServerLicenseCheckMojo extends AbstractMojo {

    private Gson gson = new Gson();

    @Component
    private MavenProject project = null;

    /**
     * This is part of the Maven settings file, and it should let developers proceed with
     * build even if Maven is being run in some kind of protected server environment.
     * 
     */
    @Parameter( defaultValue = "${settings.offline}" )
    private boolean offline;

    /**
     * The validation server, to override, include the following configuration.
     * Make sure that you include a trailing slash (restful format) or an equals sign (query string).
     * 
     * <configuration>
     *     <host>http://localhost:8081/validate.php?id=</host>
     * </configuration>
     * 
     */
    @Parameter( property = "check.host", defaultValue = "http://complykit.org/api/license-check/" )
    private String host;

    /**
     * A list of artifacts that should be excluded from consideration. Example:
     * <configuration>
     *      <excludes>
     *          <param>full:artifact:coords</param>
     *      </excludes>
     * </configuration>
     */
    @Parameter( property = "check.excludes" )
    private String[] excludes;

    /**
     * A list of blacklisted licenses. Example:
     * <configuration>
     *      <blacklist>
     *          <param>agpl-3.0</param>
     *          <param>gpl-2.0</param>
     *          <param>gpl-3.0</param>
     *      </blacklist>
     * </configuration>
     */
    @Parameter( property = "check.blacklist" )
    private String[] blacklist;

    /**
     * This is the primary entry point into the Maven plugin.
     */
    @SuppressWarnings("rawtypes")
    public void execute() throws MojoExecutionException, MojoFailureException {

        long startTime = System.currentTimeMillis();
        getLog().info("------------------------------------------------------------------------");
        getLog().info("VALIDATING OPEN SOURCE LICENSES                                         ");
        getLog().info("------------------------------------------------------------------------");
        getLog().info("This plugin will validate that the artifacts you're using have a"); 
        getLog().info("license file. When the plugin recognizes that an artifact is one"); 
        getLog().info("of the Open Source Initiative (OSI) approved licenses, it will"); 
        getLog().info("give you the URL for the license. ");
        getLog().info("");
        getLog().info("WARNING: all artifact coordinates are sent over the wire (unencrypted).");
        getLog().info("");
        getLog().info("This plugin and its author are not associated with the OSI.");
        getLog().info("Please send me feedback: me@michaelrice.com. Thanks!");

        if (offline) {
            getLog().info("*** Currently offline, skipping license validation ***");
        } else {

            Set artifacts = project.getDependencyArtifacts();
            getLog().info("Validating licenses for "+artifacts.size()+" artifact(s)");
            Iterator it=artifacts.iterator();
            while (it.hasNext()) {
                Artifact artifact = (Artifact)it.next();
                String artifactKey = artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getBaseVersion();
                if (!artifactIsOnExcludeList(artifactKey)) {
                    if (!checkArtifact(artifactKey)) {
                        throw new MojoFailureException("could not validate license for artifact "+artifactKey);
                    }
                } else {
                    getLog().info(artifactKey+" is on exclude list, skipped");
                }
            }

        }

        getLog().info("License validation complete (took "+(System.currentTimeMillis()-startTime)+"ms)");

    }

    /**
     * Just do a quick walk through of the excluded list and return true if match.
     * 
     * @param artifactKey the coordinates
     * @return true if it's on the exclude list, false if not or no exclude list was defined
     */
    private boolean artifactIsOnExcludeList(String artifactKey) {
        if (excludes!=null) {
            //walking the array should be easy since there shouldn't be more than a handful on here
            for (int i=0;i<excludes.length;i++) {
                if(artifactKey.toLowerCase().equals(excludes[i].toLowerCase()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Compare the license code to the user-provided black list
     * @param licenseCode the standardized code from complykit
     * @return true if the code is on the user-provided black list
     */
    private boolean licenseIsOnBlacklist(String licenseCode) {
        if (blacklist!=null) {
            //walking through the array should be small and fast for now
            for (int i=0; i<blacklist.length; i++) {
                if (licenseCode.toLowerCase().equals(blacklist[i].toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * This method actually runs the artifact's coordinates against a validation server.
     * Right now, all the logic is buried into this big function.
     * 
     * @param dependencyCoordinates
     * @return true if the artifact passes the checks.
     */
    //TODO as of version 0.4, this function is getting too monolithic
    public boolean checkArtifact(String dependencyCoordinates) {

        boolean result = false;
        HttpClient client = new DefaultHttpClient();
        String msg = dependencyCoordinates+" ";
        try {

            //Note that this assumes the host ends with = or /
            String url = host+dependencyCoordinates;

            HttpGet get = new HttpGet(url);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            Result serverResult = gson.fromJson(responseBody, Result.class);

            if ("ok".equals(serverResult.getLicensedDeclared())) {
                msg += "license="+serverResult.getLicense();
                if (!licenseIsOnBlacklist(serverResult.getLicense())) {
                    if (serverResult.getLicenseTitle()!=null && serverResult.getLicenseTitle().length()>0)
                        msg+= " ("+serverResult.getLicenseTitle()+")";
                    if (serverResult.getOsiLink()!=null && serverResult.getOsiLink().length()>0)
                        msg+= " "+serverResult.getOsiLink();
                    result = true;
                } else {
                    msg+=" IS ON THE BLACKLIST, failing build";
                    result = false;
                }
                getLog().info(msg);
            } else {
                if (serverResult.getLicense()==null||serverResult.getLicense().length()==0)
                    msg+=" NO OS LICENSE FOUND";
                getLog().error(msg);
                result = false;
            }

        } catch (Exception e) {
            getLog().error(e);
        } finally {
            client.getConnectionManager().shutdown();
        }

        return result;
    }

}
