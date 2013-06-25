license-check backlog
==========
[ ] default to fail unless the plugin user confirms that they are ok with the system sending dependency coordinates to the server
[ ] switch to allow users to stop running every license against the server, first check a local resources file for a mapping to at least an open source license
[ ] switch to a batched mode so the plugin just sends the list one time
[ ] cache locally
[ ] create a mapping file that lets you override a license description locally (i.e., if you have internal dependencies, you don't want the build to fail because you're using your own copyright) or skip certain artifacts in the build
[ ] add a local file that lets you configure the licenses that your organization is willing to accept