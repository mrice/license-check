package com.michaelrice.licensecheck.mojo;

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

import com.google.gson.Gson;
import com.michaelrice.licensecheck.model.Result;

@Mojo(name="check")
public class LicenseCheckMojo extends AbstractMojo {

    private Gson gson = new Gson();

    @Component
    private MavenProject project = null;

    @Parameter( defaultValue = "${settings.offline}" )
    private boolean offline;

    @SuppressWarnings("rawtypes")
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("------------------------------------------------------------------------");
        getLog().info("VALIDATING LICENSES");
        getLog().info("------------------------------------------------------------------------");
        if (offline) {
            getLog().info("currently offline, skipping this step");
        } else {
            Set artifacts = project.getDependencyArtifacts();
            getLog().info("Found "+artifacts.size()+" artifacts");
            Iterator it=artifacts.iterator();
            while (it.hasNext()) {
                Artifact artifact = (Artifact)it.next();
                String artifactKey = artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getBaseVersion();
                getLog().info(artifactKey + "...");
                if (!runOnlineArtifactCheck(artifactKey)) {
                    throw new MojoFailureException("could not validate license for artifact "+artifactKey);
                }
            }
        }

    }

    public boolean runOnlineArtifactCheck(String artifactId) {

        boolean result = false;
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet("http://localhost:8081/validate.php?id="+artifactId);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            
            Result serverResult = gson.fromJson(responseBody, Result.class);
            if ("ok".equals(serverResult.getResult())) {
                result = true;
                String msg = "..."+serverResult.getResult()+": "+serverResult.getLicense();
                getLog().info(msg);
            } else {
                result = false;
                String msg = "..."+serverResult.getResult()+": ";
                if (serverResult.getLicense()==null||serverResult.getLicense().length()==0)
                    msg+=": NO LICENSE FOUND";
                else
                    msg+=": "+serverResult.getLicense();
                getLog().error(msg);
            }

        } catch (Exception e) {
            getLog().error(e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        
        return result;
    }

}
