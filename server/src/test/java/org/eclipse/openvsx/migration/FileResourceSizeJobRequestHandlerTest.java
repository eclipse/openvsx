package org.eclipse.openvsx.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.storage.FileNotFoundInStorageException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileResourceSizeJobRequestHandlerTest {

    @Mock
    MigrationService migrations;

    @Test
    void run_leavesSizeUnsetAndDoesNotThrowWhenTheObjectIsMissingFromStorage() throws Exception {
        var resource = fileResource();
        var jobRequest = new MigrationJobRequest<>(FileResourceSizeJobRequestHandler.class, 1L);
        when(migrations.getResource(jobRequest)).thenReturn(resource);
        when(migrations.getFileSize(resource)).thenThrow(new FileNotFoundInStorageException("gone"));

        var handler = new FileResourceSizeJobRequestHandler(migrations);

        assertThatCode(() -> handler.run(jobRequest)).doesNotThrowAnyException();

        verify(migrations, never()).updateResource(any());
    }

    @Test
    void run_recordsTheSizeReturnedByStorage() throws Exception {
        var resource = fileResource();
        var jobRequest = new MigrationJobRequest<>(FileResourceSizeJobRequestHandler.class, 1L);
        when(migrations.getResource(jobRequest)).thenReturn(resource);
        when(migrations.getFileSize(resource)).thenReturn(42L);

        new FileResourceSizeJobRequestHandler(migrations).run(jobRequest);

        verify(migrations).updateResource(resource);
    }

    private FileResource fileResource() {
        var namespace = new Namespace();
        namespace.setName("foo");

        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform("universal");
        extVersion.setExtension(extension);

        var resource = new FileResource();
        resource.setName("extension.vsix");
        resource.setExtension(extVersion);
        return resource;
    }
}
