/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class PublishExtensionVersionJobRequest implements JobRequest {

    private String namespaceName;
    private String extensionName;
    private String targetPlatform;
    private String version;

    public PublishExtensionVersionJobRequest() { }

    public PublishExtensionVersionJobRequest(String namespaceName, String extensionName, String targetPlatform, String version) {
        this.namespaceName = namespaceName;
        this.extensionName = extensionName;
        this.targetPlatform = targetPlatform;
        this.version = version;
    }

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return PublishExtensionVersionJobRequestHandler.class;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
