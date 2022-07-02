# Eclipse Open VSX

[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/eclipse/openvsx)
[![Join the chat at https://gitter.im/eclipse/openvsx](https://badges.gitter.im/eclipse/openvsx.svg)](https://gitter.im/eclipse/openvsx)
[![CI](https://github.com/eclipse/openvsx/workflows/CI/badge.svg)](https://github.com/eclipse/openvsx/actions?query=workflow%3ACI)

Open VSX is a [vendor-neutral](https://projects.eclipse.org/projects/ecd.openvsx) open-source alternative to the [Visual Studio Marketplace](https://marketplace.visualstudio.com/vscode). It provides a server application that manages [VS Code extensions](https://code.visualstudio.com/api) in a database, a web application similar to the VS Code Marketplace, and a command-line tool for publishing extensions similar to [vsce](https://code.visualstudio.com/api/working-with-extensions/publishing-extension#vsce).

A public instance of Open VSX is running at [open-vsx.org](https://open-vsx.org/). Please report issues related to that instance at [EclipseFdn/open-vsx.org](https://github.com/EclipseFdn/open-vsx.org).

## Getting Started

See the [openvsx Wiki](https://github.com/eclipse/openvsx/wiki) for documentation of general concepts and usage of this project.

## Development

The easiest way to get a development environment for this project is to open it in [Gitpod](https://gitpod.io/).

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/eclipse/openvsx)

Click _Open Browser_ on port 3000 to see the running web application.

### cli

 * `yarn build` &mdash; build the library and `ovsx` command
 * `yarn watch` &mdash; watch (build continuously)

The command line tool is available at `cli/lib/ovsx`.

### webui

The default frontend is the one bundled in the Docker image, and is also used for testing in the development environment. It depends on the compiled library, so make sure to build or watch the library before you build or watch the default frontend.

 * `yarn build` &mdash; build the library
 * `yarn watch` &mdash; watch (build continuously)
 * `yarn build:default` &mdash; build the default frontend (run webpack)
 * `yarn watch:default` &mdash; run webpack in watch mode
 * `yarn start:default` &mdash; start Express to serve the frontend on port 3000

 The Express server is started automatically in Gitpod. A restart is usually not necessary.

### server

 * `./gradlew build` &mdash; build and test the server
 * `./gradlew assemble -t` &mdash; build continuously (the server is restarted after every change)
 * `./gradlew runServer` &mdash; start the Spring server on port 8080
 * `./scripts/test-report.sh` &mdash; display test results on port 8081

The Spring server is started automatically in Gitpod. It includes `spring-boot-devtools` which detects changes in the compiled class files and restarts the server.

### OAuth Setup

If you would like to test authorization through GitHub, you need to [create an OAuth app](https://developer.github.com/apps/building-oauth-apps/creating-an-oauth-app/) with a callback URL pointing to the exposed port 8080 of your Gitpod workspace. You can get it by calling a script:

```
server/scripts/callback-url.sh github
```

Note that the callback URL needs to be [updated on GitHub](https://github.com/settings/developers) whenever you create a fresh Gitpod workspace.

After you created the GitHub OAuth app, the next step is to copy the _Client ID_ and _Client Secret_ into [Gitpod environment variables](https://www.gitpod.io/docs/environment-variables/) named `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` and bound to this repository. If you change the variables in a running workspace, run `scripts/generate-properties.sh` in the `server` directory to update the application properties.

With these settings in place, you should be able to log in by authorizing your OAuth app.

### Google Cloud Setup

If you would like to test file storage via Google Cloud, follow these steps:

 * Create a [GCP](https://cloud.google.com/) project and a bucket.
 * Make the bucket public by granting the role "Storage Object Viewer" to `allUsers`.
 * [Configure CORS](https://cloud.google.com/storage/docs/configuring-cors#configure-cors-bucket) on the bucket with origin `"*"` and method `"GET"`.
 * Create environment variables named `GCP_PROJECT_ID` and `GCS_BUCKET_ID` containing your GCP project and bucket identifiers. If you change the variables in a running workspace, run `scripts/generate-properties.sh` in the `server` directory to update the application properties.
 * Create a GCP service account with role "Storage Object Admin" and copy its credentials file into your workspace.
 * Create an environment variable `GOOGLE_APPLICATION_CREDENTIALS` containing the path to the credentials file.

### Azure Setup

If you would like to test file storage via Azure Blob, follow these steps:

 * Create a file [storage account](https://portal.azure.com/) and a container named `openvsx-resources` (a different name is possible if you change the `ovsx.storage.azure.blob-container` property).
 * Allow Blob public access in the storage account and set the container's public access level to "Blob".
 * Configure CORS in your storage account with origin `"*"`, method `"GET"` and allowed headers `"x-market-client-id, x-market-user-id, x-client-name, x-client-version, x-machine-id, x-client-commit"`
 * Create an environment variable `AZURE_SERVICE_ENDPOINT` with the "Blob service" URL of your storage account. If you change the variables in a running workspace, run `scripts/generate-properties.sh` in the `server` directory to update the application properties.
 * Generate a "Shared access signature" and put its token into an environment variable `AZURE_SAS_TOKEN`.

If you also would like to test download count via Azure Blob, follow these steps:

* Create an additional [storage account](https://portal.azure.com/) for diagnostics logging.
  * IMPORTANT: set the same location as the file storage account (e.g. North Europe).
  * Disable Blob public access.
* In the file storage account
  * Open the diagnostic settings (`Monitoring` -> `Diagnostic settings (preview)`).
    * Click `blob`.
    * Click `Add diagnostic setting`.
    * Select `StorageRead`, `Transaction` and `Archive to a storage account`.
    * Select the diagnostic storage account you created in the previous step as `Storage account`.
* Back to the diagnostic storage account
  * Navigate to `Data Storage`-> `Containers`
    * The `insights-logs-storageread` container should have been added (it might take a few minutes and you might need to do some test downloads or it won't get created).
    * Create a "Shared access token" for the `insights-logs-storageread` container.
      * Click on the `insights-logs-storageread` container.
        * Click on `Settings` -> `Shared access token`
          * Must have `Read` and `List` permissions.
          * Set the expiry date to a reasonable value
          * Set the "Allowed IP Addresses" to the server's IP address.
  * Go to `Data Management`-> `Lifecycle management`
    * Create a rule, so that logs don't pile up and the download count service stays performant.
    * Select `Limit blobs with filters`, `Block blobs` and `Base blobs`.
    * Pick number of days (e.g. 7).
    * Enter `insights-logs-storageread/resourceId=` blob prefix to limit the rule to the `insights-logs-storageread` container.
* You need to add two environment variables to your server environment
  * `AZURE_LOGS_SERVICE_ENDPOINT` with the "Blob service" URL of your diagnostic storage account. The URL must end with a slash!
  * `AZURE_LOGS_SAS_TOKEN` with the shared access token for the `insights-logs-storageread` container.
  * If you change the variables in a running workspace, run `scripts/generate-properties.sh` in the `server` directory to update the application properties.

### Amazon S3 Setup

If you would like to test file storage via Amazon S3, follow these steps:

* Login to the AWS Console and create an S3 [storage bucket](https://s3.console.aws.amazon.com/s3/home?refid=ft_card)
* Follow the steps for [programmatic access](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys) to create your access key id and secret access key
* Configure the following environment variables on your server environment
  * `AWS_ACCESS_KEY_ID` with your access key id
  * `AWS_SECRET_ACCESS_KEY` with your secret access key
  * `AWS_REGION` with your bucket region name
  * `AWS_SERVICE_ENDPOINT` with the url of your S3 provider if not using AWS (for AWS do not set)
  * `AWS_BUCKET` with your bucket name
  * `AWS_PATH_STYLE_ACCESS` whether or not to use path style access, (defaults to `false`)
    * Path-style access: `https://s3.<region>.amazonaws.com/<bucket-name>/<resource-key>`
    * Virtual-style access: `https://<bucket-name>.s3.<region>.amazonaws.com/<resource-key>`

## License

[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/)
