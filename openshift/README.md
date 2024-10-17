# Deploying a Private OpenVSX Registry on OpenShift

This guide provides the necessary steps to deploy a private OpenVSX registry on OpenShift. By following the instructions, you'll have a fully operational OpenVSX server and CLI deployed within your OpenShift cluster, ready to be used by Eclipse Che or other services.

## Prerequisites
- [Deploy and run Eclipse Che on your cluster](https://eclipse.dev/che/docs/stable/administration-guide/installing-che-in-the-cloud/)
- [Create a workspace](https://eclipse.dev/che/docs/stable/end-user-guide/starting-a-workspace-from-a-git-repository-url/) from the [openvsx Git repository URL](https://github.com/eclipse/openvsx)  

## Step-by-Step Instructions
In the workspace, you'll find a set of predefined commands from the `devfile.yaml` that will assist you in preparing and deploying a private OpenVSX registry. To execute any of these commands within your workspace, navigate to Terminal -> Run Task -> devfile:

* 2.1. OC log-in

Make sure you are logged in to your OpenShift cluster as a cluster admin:
```
oc login --username=your-admin-username --password=your-admin-password
```
* 2.1. Build and Publish OpenVSX Image

Build the OpenVSX image and push it to the OpenShift internal registry. You'll ask to enter the OpenVSX version to deploy (default is v0.17.0).
* 2.2. Build and Publish OpenVSX CLI Image

Build the OpenVSX CLI image and push it to the OpenShift internal registry.
* 2.3. Deploy OpenVSX

Deploy the OpenVSX registry using the provided `openvsx-deployment.yml` template
* 2.4. Configure Che to use the internal Open VSX registry

In case you have deployed Eclipse Che on the cluster, you can patch it to use your private OpenVSX registry.

## OpenShift Template (openvsx-deployment.yml)
You can find the deployment YAML configuration in the `openvsx-deployment.yml` file. This template defines the deployments, services, and route for the PostgreSQL database, Elasticsearch, OpenVSX Server, and OpenVSX CLI.

### Important Parameters
* `ROUTE_NAME`: The name of the route to access the OpenVSX server (default: internal)
* `NAMESPACE`: The namespace where OpenVSX will be deployed (default: openvsx)
* `POSTGRESQL_IMAGE`: The PostgreSQL image to use for the database (default: image-registry.openshift-image-registry.svc:5000/openshift/postgresql:15-el8)
* `OPENVSX_ELASTICSEARCH_IMAGE`: The image for Elasticsearch (default: docker.elastic.co/elasticsearch/elasticsearch:8.7.1).
* `OPENVSX_SERVER_IMAGE`: The image for the OpenVSX Server.
* `OPENVSX_CLI_IMAGE`: The image for the OpenVSX CLI.
* `GITHUB_CLIENT_ID_BASE64` and `GITHUB_CLIENT_SECRET_BASE64`: Base64 encoded GitHub Client ID and Secret to setup GitHub OAuth.
