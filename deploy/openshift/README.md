# Deploying a Private OpenVSX Registry on OpenShift

This guide provides the necessary steps to deploy a private OpenVSX registry on OpenShift. By following the instructions, you'll have a fully operational OpenVSX server and CLI deployed within your OpenShift cluster, ready to be used by Eclipse Che or other services.

## Prerequisites
- [Deploy and run Eclipse Che on your cluster](https://eclipse.dev/che/docs/stable/administration-guide/installing-che-in-the-cloud/)
- [Create a workspace](https://eclipse.dev/che/docs/stable/end-user-guide/starting-a-workspace-from-a-git-repository-url/) from the [openvsx Git repository URL](https://github.com/eclipse/openvsx)  

## Step-by-Step Instructions
In the workspace, you'll find a set of predefined commands from the `devfile.yaml` that will assist you in preparing and deploying a private OpenVSX registry. To execute any of these commands within your workspace, navigate to Terminal -> Run Task -> devfile:

* 2.1. Create Namespace for OpenVSX
Creates a namespace on the OpenShift cluster for deploying OpenVSX (default: openvsx).

* 2.2. Build and Publish OpenVSX Image

Build the OpenVSX image and push it to the OpenShift internal registry. You'll ask to enter the OpenVSX version to deploy (default is v0.27.0).

* 2.3. Build and Publish OpenVSX CLI Image

Build the OpenVSX CLI image and push it to the OpenShift internal registry. You'll ask to enter the `ovsx` version to deploy (default is 0.10.5).

* 2.4. Deploy OpenVSX

Deploy the OpenVSX registry using the provided `openvsx-deployment.yml` template

* 2.5. Add OpenVSX user with PAT to the DB

This command adds a new OpenVSX user along with a Personal Access Token (PAT) to the PostgreSQL database.

* 2.6. Configure Che to use the internal OpenVSX registry

In case you have deployed Eclipse Che on the cluster, you can patch it to use your private OpenVSX registry.

* 2.7. Publish a VS Code Extension from a URL

This command facilitates publishing a VS Code extension to the local OpenVSX registry. It prompts the user to provide the extension's namespace name and download URL. The extension is then downloaded into a temporary folder inside the ovsx-cli pod, a namespace is created (if not already present), and the extension is published to the OpenVSX registry. Afterward, the temporary file is deleted. This command is ideal for adding custom or internal extensions to a local OpenVSX instance.

* 2.8. Publish a VS Code Extension from a VSIX file

This command publishes a VS Code extension to the local OpenVSX registry directly from a VSIX file. It prompts the user to provide the extension’s namespace name and the path to the VSIX file. The file is uploaded into the pod where ovsx is installed, a namespace is created if it does not already exist, and the extension is published to the OpenVSX registry. Once the process completes, the temporary VSIX file is removed from the pod. This method is ideal for distributing locally built or manually obtained VSIX packages to a private OpenVSX instance.

* 2.9. Publish list of VS Code Extensions

This command facilitates publishing a list of VS Code extensions to a local OpenVSX registry based on URLs specified in the `extensions.txt` file. For each extension listed, it downloads the `.vsix` file into a temporary directory on the ovsx-cli pod, creates a namespace if it doesn’t already exist, and publishes the extension to the OpenVSX registry. After each extension is published, the temporary file is deleted from the pod. This command is ideal for managing multiple extensions by automating the download, namespace creation, and publishing steps, making it easy to maintain a custom set of extensions in a local OpenVSX instance.

## OpenShift Template (openvsx-deployment.yml)
You can find the deployment YAML configuration in the `openvsx-deployment.yml` file. This template defines the deployments, services, and route for the PostgreSQL database, Elasticsearch, OpenVSX Server, and OpenVSX CLI.

### Important Parameters
* `ROUTE_NAME`: The name of the route to access the OpenVSX server (default: internal)
* `NAMESPACE`: The namespace where OpenVSX will be deployed (default: openvsx)
* `POSTGRESQL_IMAGE`: The PostgreSQL image to use for the database (default: image-registry.openshift-image-registry.svc:5000/openshift/postgresql:15-el8)
* `OPENVSX_ELASTICSEARCH_IMAGE`: The image for Elasticsearch (default: docker.elastic.co/elasticsearch/elasticsearch:8.7.1).
* `OPENVSX_SERVER_IMAGE`: The image for the OpenVSX Server.
* `OPENVSX_CLI_IMAGE`: The image for the OpenVSX CLI.
* `OVSX_PAT_BASE64`: Base64 encoded OVSX personal access token.
* `GITHUB_CLIENT_ID_BASE64` and `GITHUB_CLIENT_SECRET_BASE64`: Base64 encoded GitHub Client ID and Secret to setup GitHub OAuth.
