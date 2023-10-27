/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
public class VSCodeIdUpdateServiceTest {

    @MockBean
    RepositoryService repositories;

    @MockBean
    VSCodeIdService idService;

    @Autowired
    VSCodeIdUpdateService updateService;

    @Test
    public void testUpdateAllNoChanges() throws InterruptedException {
        var namespaceName1 = "foo";
        var namespacePublicId1 = UUID.randomUUID().toString();
        var extensionName1 = "bar";
        var extensionPublicId1 = UUID.randomUUID().toString();

        var namespace1 = new Namespace();
        namespace1.setId(1L);
        namespace1.setName(namespaceName1);
        namespace1.setPublicId(namespacePublicId1);

        var extension1 = new Extension();
        extension1.setId(2L);
        extension1.setName(extensionName1);
        extension1.setNamespace(namespace1);
        extension1.setPublicId(extensionPublicId1);

        var namespaceName2 = "baz";
        var namespacePublicId2 = UUID.randomUUID().toString();
        var extensionName2 = "foobar";
        var extensionPublicId2 = UUID.randomUUID().toString();

        var namespace2 = new Namespace();
        namespace2.setId(3L);
        namespace2.setName(namespaceName2);
        namespace2.setPublicId(namespacePublicId2);

        var extension2 = new Extension();
        extension2.setId(4L);
        extension2.setName(extensionName2);
        extension2.setPublicId(extensionPublicId2);
        extension2.setNamespace(namespace2);

        var namespaceName3 = "baz2";
        var namespacePublicId3 = UUID.randomUUID().toString();
        var extensionName3 = "foobar2";
        var extensionPublicId3 = UUID.randomUUID().toString();

        var namespace3 = new Namespace();
        namespace3.setId(5L);
        namespace3.setName(namespaceName3);
        namespace3.setPublicId(namespacePublicId3);

        var extension3 = new Extension();
        extension3.setId(6L);
        extension3.setName(extensionName3);
        extension3.setPublicId(extensionPublicId3);
        extension3.setNamespace(namespace3);

        Mockito.when(repositories.findAllPublicIds()).thenReturn(List.of(extension1, extension2, extension3));
        updateService.updateAll();
        Mockito.verify(repositories, Mockito.never()).updateExtensionPublicIds(Mockito.anyMap());
        Mockito.verify(repositories, Mockito.never()).updateNamespacePublicIds(Mockito.anyMap());
    }

    @Test
    public void testUpdateAllRandomNoChanges() throws InterruptedException {
        var namespaceName1 = "foo";
        var namespacePublicId1 = UUID.randomUUID().toString();
        var extensionName1 = "bar";
        var extensionPublicId1 = UUID.randomUUID().toString();

        var namespace1 = new Namespace();
        namespace1.setId(1L);
        namespace1.setName(namespaceName1);
        namespace1.setPublicId(namespacePublicId1);

        var extension1 = new Extension();
        extension1.setId(2L);
        extension1.setName(extensionName1);
        extension1.setNamespace(namespace1);
        extension1.setPublicId(extensionPublicId1);

        var namespaceName2 = "baz";
        var namespacePublicId2 = UUID.randomUUID().toString();
        var extensionName2 = "foobar";
        var extensionPublicId2 = UUID.randomUUID().toString();

        var namespace2 = new Namespace();
        namespace2.setId(3L);
        namespace2.setName(namespaceName2);
        namespace2.setPublicId(namespacePublicId2);

        var extension2 = new Extension();
        extension2.setId(4L);
        extension2.setName(extensionName2);
        extension2.setPublicId(extensionPublicId2);
        extension2.setNamespace(namespace2);

        var namespaceName3 = "baz2";
        var namespacePublicId3 = UUID.randomUUID().toString();
        var extensionName3 = "foobar2";
        var extensionPublicId3 = UUID.randomUUID().toString();

        var namespace3 = new Namespace();
        namespace3.setId(5L);
        namespace3.setName(namespaceName3);
        namespace3.setPublicId(namespacePublicId3);

        var extension3 = new Extension();
        extension3.setId(6L);
        extension3.setName(extensionName3);
        extension3.setPublicId(extensionPublicId3);
        extension3.setNamespace(namespace3);

        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension1);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension2);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension3);
        Mockito.when(repositories.findAllPublicIds()).thenReturn(List.of(extension1, extension2, extension3));

        updateService.updateAll();
        Mockito.verify(repositories, Mockito.never()).updateExtensionPublicIds(Mockito.anyMap());
        Mockito.verify(repositories, Mockito.never()).updateNamespacePublicIds(Mockito.anyMap());
    }

    @Test
    public void testUpdateAllChange() throws InterruptedException {
        var namespaceName1 = "foo";
        var namespacePublicId1 = UUID.randomUUID().toString();
        var extensionName1 = "bar";
        var extensionPublicId1 = UUID.randomUUID().toString();

        var namespace1 = new Namespace();
        namespace1.setId(1L);
        namespace1.setName(namespaceName1);
        namespace1.setPublicId(namespacePublicId1);

        var extension1 = new Extension();
        extension1.setId(2L);
        extension1.setName(extensionName1);
        extension1.setNamespace(namespace1);
        extension1.setPublicId(extensionPublicId1);

        var namespaceName2 = "baz";
        var namespacePublicId2 = UUID.randomUUID().toString();
        var extensionName2 = "foobar";
        var extensionPublicId2 = UUID.randomUUID().toString();

        var namespace2 = new Namespace();
        namespace2.setId(3L);
        namespace2.setName(namespaceName2);
        namespace2.setPublicId(namespacePublicId2);

        var extension2 = new Extension();
        extension2.setId(4L);
        extension2.setName(extensionName2);
        extension2.setPublicId(extensionPublicId2);
        extension2.setNamespace(namespace2);

        var namespaceName3 = "baz2";
        var namespacePublicId3 = UUID.randomUUID().toString();
        var extensionName3 = "foobar2";
        var extensionPublicId3 = UUID.randomUUID().toString();

        var namespace3 = new Namespace();
        namespace3.setId(5L);
        namespace3.setName(namespaceName3);
        namespace3.setPublicId(namespacePublicId3);

        var extension3 = new Extension();
        extension3.setId(6L);
        extension3.setName(extensionName3);
        extension3.setPublicId(extensionPublicId3);
        extension3.setNamespace(namespace3);

        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension1);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionPublicId3);
            ext.getNamespace().setPublicId(namespacePublicId3);
            return null;
        }).when(idService).getUpstreamPublicIds(extension2);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension3);
        Mockito.when(repositories.findAllPublicIds()).thenReturn(List.of(extension1, extension2, extension3));

        var extensionUuid = UUID.randomUUID();
        var namespaceUuid = UUID.randomUUID();
        var uuidMock = Mockito.mockStatic(UUID.class);
        uuidMock.when(UUID::randomUUID).thenReturn(extensionUuid, namespaceUuid);

        updateService.updateAll();
        Mockito.verify(repositories, Mockito.times(1)).updateExtensionPublicIds(Map.of(
                extension2.getId(), extensionPublicId3,
                extension3.getId(), extensionUuid.toString()
        ));
        Mockito.verify(repositories, Mockito.times(1)).updateNamespacePublicIds(Map.of(
                namespace2.getId(), namespacePublicId3,
                namespace3.getId(), namespaceUuid.toString()
        ));
        uuidMock.close();
    }

    @Test
    public void testUpdateUpstream() throws InterruptedException {
        var namespaceName = "foo";
        var namespacePublicId = "123-456-789";
        var extensionName = "bar";
        var extensionPublicId = "abc-def-ghi";

        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setName(namespaceName);

        var extension = new Extension();
        extension.setId(2L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);

        Mockito.when(repositories.findPublicId(namespaceName, extensionName)).thenReturn(extension);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionPublicId);
            ext.getNamespace().setPublicId(namespacePublicId);
            return null;
        }).when(idService).getUpstreamPublicIds(extension);

        updateService.update(namespaceName, extensionName);
        Mockito.verify(repositories).updateExtensionPublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(extension.getId()).equals(extensionPublicId);
        }));
        Mockito.verify(repositories).updateNamespacePublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(namespace.getId()).equals(namespacePublicId);
        }));
    }

    @Test
    public void testUpdateRandom() throws InterruptedException {
        var namespaceName = "foo";
        var namespaceUuid = UUID.randomUUID();
        var extensionName = "bar";
        var extensionUuid = UUID.randomUUID();

        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setName(namespaceName);

        var extension = new Extension();
        extension.setId(2L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);

        Mockito.when(repositories.findPublicId(namespaceName, extensionName)).thenReturn(extension);
        var uuidMock = Mockito.mockStatic(UUID.class);
        uuidMock.when(UUID::randomUUID).thenReturn(extensionUuid, namespaceUuid);

        updateService.update(namespaceName, extensionName);
        Mockito.verify(repositories).updateExtensionPublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(extension.getId()).equals(extensionUuid.toString());
        }));
        Mockito.verify(repositories).updateNamespacePublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(namespace.getId()).equals(namespaceUuid.toString());
        }));
        uuidMock.close();
    }

    @Test
    public void testUpdateRandomExists() throws InterruptedException {
        var namespaceName = "foo";
        var namespaceUuid1 = UUID.randomUUID();
        var namespaceUuid2 = UUID.randomUUID();
        var extensionName = "bar";
        var extensionUuid1 = UUID.randomUUID();
        var extensionUuid2 = UUID.randomUUID();

        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setName(namespaceName);

        var extension = new Extension();
        extension.setId(2L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);

        Mockito.when(repositories.findPublicId(namespaceName, extensionName)).thenReturn(extension);
        Mockito.when(repositories.extensionPublicIdExists(extensionUuid1.toString())).thenReturn(true);
        Mockito.when(repositories.namespacePublicIdExists(namespaceUuid1.toString())).thenReturn(true);
        var uuidMock = Mockito.mockStatic(UUID.class);
        uuidMock.when(UUID::randomUUID)
                .thenReturn(extensionUuid1, extensionUuid2, namespaceUuid1, namespaceUuid2);

        updateService.update(namespaceName, extensionName);
        Mockito.verify(repositories).updateExtensionPublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(extension.getId()).equals(extensionUuid2.toString());
        }));
        Mockito.verify(repositories).updateNamespacePublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 1 && map.get(namespace.getId()).equals(namespaceUuid2.toString());
        }));
        uuidMock.close();
    }

    @Test
    public void testUpdateDuplicateUpstreamChanges() throws InterruptedException {
        var namespaceName1 = "foo";
        var namespacePublicId1 = UUID.randomUUID().toString();
        var extensionName1 = "bar";
        var extensionPublicId1 = UUID.randomUUID().toString();

        var namespace1 = new Namespace();
        namespace1.setId(1L);
        namespace1.setName(namespaceName1);

        var extension1 = new Extension();
        extension1.setId(2L);
        extension1.setName(extensionName1);
        extension1.setNamespace(namespace1);

        var namespaceName2 = "baz";
        var namespaceUuid2 = UUID.randomUUID();
        var extensionName2 = "foobar";
        var extensionUuid2 = UUID.randomUUID();

        var namespace2 = new Namespace();
        namespace2.setId(3L);
        namespace2.setName(namespaceName2);
        namespace2.setPublicId(namespacePublicId1);

        var extension2 = new Extension();
        extension2.setId(4L);
        extension2.setName(extensionName2);
        extension2.setPublicId(extensionPublicId1);
        extension2.setNamespace(namespace2);

        var namespaceName3 = "baz2";
        var namespaceUuid3 = UUID.randomUUID();
        var extensionName3 = "foobar2";
        var extensionUuid3 = UUID.randomUUID();

        var namespace3 = new Namespace();
        namespace3.setId(5L);
        namespace3.setName(namespaceName3);
        namespace3.setPublicId(namespaceUuid2.toString());

        var extension3 = new Extension();
        extension3.setId(6L);
        extension3.setName(extensionName3);
        extension3.setPublicId(extensionUuid2.toString());
        extension3.setNamespace(namespace3);

        var namespaceName4 = "baz3";
        var namespaceUuid4 = UUID.randomUUID();
        var extensionName4 = "foobar3";
        var extensionUuid4 = UUID.randomUUID();

        var namespace4 = new Namespace();
        namespace4.setId(7L);
        namespace4.setName(namespaceName4);
        namespace4.setPublicId(namespaceUuid3.toString());

        var extension4 = new Extension();
        extension4.setId(8L);
        extension4.setName(extensionName4);
        extension4.setPublicId(extensionUuid3.toString());
        extension4.setNamespace(namespace4);

        Mockito.when(repositories.findPublicId(namespaceName1, extensionName1)).thenReturn(extension1);
        Mockito.when(repositories.findPublicId(extensionPublicId1)).thenReturn(extension2);
        Mockito.when(repositories.findNamespacePublicId(namespacePublicId1)).thenReturn(extension2);
        Mockito.when(repositories.findPublicId(extensionUuid2.toString())).thenReturn(extension3);
        Mockito.when(repositories.findNamespacePublicId(namespaceUuid2.toString())).thenReturn(extension3);
        Mockito.when(repositories.findPublicId(extensionUuid3.toString())).thenReturn(extension4);
        Mockito.when(repositories.findNamespacePublicId(namespaceUuid3.toString())).thenReturn(extension4);

        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionPublicId1);
            ext.getNamespace().setPublicId(namespacePublicId1);
            return null;
        }).when(idService).getUpstreamPublicIds(extension1);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionUuid2.toString());
            ext.getNamespace().setPublicId(namespaceUuid2.toString());
            return null;
        }).when(idService).getUpstreamPublicIds(extension2);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionUuid3.toString());
            ext.getNamespace().setPublicId(namespaceUuid3.toString());
            return null;
        }).when(idService).getUpstreamPublicIds(extension3);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionUuid4.toString());
            ext.getNamespace().setPublicId(namespaceUuid4.toString());
            return null;
        }).when(idService).getUpstreamPublicIds(extension4);

        updateService.update(namespaceName1, extensionName1);
        Mockito.verify(repositories).updateExtensionPublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 4
                    && map.get(extension1.getId()).equals(extensionPublicId1)
                    && map.get(extension2.getId()).equals(extensionUuid2.toString())
                    && map.get(extension3.getId()).equals(extensionUuid3.toString())
                    && map.get(extension4.getId()).equals(extensionUuid4.toString());
        }));
        Mockito.verify(repositories).updateNamespacePublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 4
                    && map.get(namespace1.getId()).equals(namespacePublicId1)
                    && map.get(namespace2.getId()).equals(namespaceUuid2.toString())
                    && map.get(namespace3.getId()).equals(namespaceUuid3.toString())
                    && map.get(namespace4.getId()).equals(namespaceUuid4.toString());
        }));
    }

    @Test
    public void testUpdateDuplicateRandom() throws InterruptedException {
        var namespaceName1 = "foo";
        var namespacePublicId1 = UUID.randomUUID().toString();
        var extensionName1 = "bar";
        var extensionPublicId1 = UUID.randomUUID().toString();

        var namespace1 = new Namespace();
        namespace1.setId(1L);
        namespace1.setName(namespaceName1);

        var extension1 = new Extension();
        extension1.setId(2L);
        extension1.setName(extensionName1);
        extension1.setNamespace(namespace1);

        var namespaceName2 = "baz";
        var namespaceUuid2 = UUID.randomUUID();
        var extensionName2 = "foobar";
        var extensionUuid2 = UUID.randomUUID();

        var namespace2 = new Namespace();
        namespace2.setId(3L);
        namespace2.setName(namespaceName2);
        namespace2.setPublicId(namespacePublicId1);

        var extension2 = new Extension();
        extension2.setId(4L);
        extension2.setName(extensionName2);
        extension2.setPublicId(extensionPublicId1);
        extension2.setNamespace(namespace2);

        Mockito.when(repositories.findPublicId(namespaceName1, extensionName1)).thenReturn(extension1);
        Mockito.when(repositories.findPublicId(extensionPublicId1)).thenReturn(extension2);
        Mockito.when(repositories.findNamespacePublicId(namespacePublicId1)).thenReturn(extension2);

        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionPublicId1);
            ext.getNamespace().setPublicId(namespacePublicId1);
            return null;
        }).when(idService).getUpstreamPublicIds(extension1);
        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(null);
            ext.getNamespace().setPublicId(null);
            return null;
        }).when(idService).getUpstreamPublicIds(extension2);
        var uuidMock = Mockito.mockStatic(UUID.class);
        uuidMock.when(UUID::randomUUID).thenReturn(extensionUuid2, namespaceUuid2);

        updateService.update(namespaceName1, extensionName1);
        Mockito.verify(repositories).updateExtensionPublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 2
                    && map.get(extension1.getId()).equals(extensionPublicId1)
                    && map.get(extension2.getId()).equals(extensionUuid2.toString());
        }));
        Mockito.verify(repositories).updateNamespacePublicIds(Mockito.argThat((Map<Long, String> map) -> {
            return map.size() == 2
                    && map.get(namespace1.getId()).equals(namespacePublicId1)
                    && map.get(namespace2.getId()).equals(namespaceUuid2.toString());
        }));
        uuidMock.close();
    }

    @Test
    public void testUpdateWaitsUntilUpdateAllIsFinished() throws InterruptedException {
        var namespaceName = "foo";
        var namespacePublicId = UUID.randomUUID().toString();
        var extensionName = "bar";
        var extensionPublicId = UUID.randomUUID().toString();

        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setName(namespaceName);

        var extension = new Extension();
        extension.setId(2L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);

        Mockito.doAnswer(invocation -> {
            var ext = invocation.getArgument(0, Extension.class);
            ext.setPublicId(extensionPublicId);
            ext.getNamespace().setPublicId(namespacePublicId);
            return null;
        }).when(idService).getUpstreamPublicIds(extension);
        Mockito.when(repositories.findPublicId(namespaceName, extensionName)).thenReturn(extension);
        Mockito.when(repositories.findAllPublicIds()).thenAnswer(invocation -> {
            Thread.sleep(1000);
            return List.of(extension);
        });

        var executor = Executors.newFixedThreadPool(2);
        var future1 = CompletableFuture.runAsync(() -> {
            try {
                updateService.updateAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
        var future2 = CompletableFuture.runAsync(() -> {
            try {
                updateService.update("foo", "bar");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture.allOf(future1, future2).join();
        var order = Mockito.inOrder(repositories);
        order.verify(repositories).findAllPublicIds();
        order.verify(repositories).updateExtensionPublicIds(Mockito.anyMap());
        order.verify(repositories).updateNamespacePublicIds(Mockito.anyMap());
        order.verify(repositories).findPublicId(namespaceName, extensionName);
        order.verify(repositories).updateExtensionPublicIds(Mockito.anyMap());
        order.verify(repositories).updateNamespacePublicIds(Mockito.anyMap());
    }

    @Test
    public void testUpdateTimeout() throws InterruptedException {
        Mockito.when(repositories.findAllPublicIds()).thenAnswer(invocation -> {
            Thread.sleep(20000);
            return Collections.emptyList();
        });

        var executor = Executors.newFixedThreadPool(2);
        var future1 = CompletableFuture.runAsync(() -> {
            try {
                updateService.updateAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
        var future2 = CompletableFuture.runAsync(() -> {
            assertThrows(RuntimeException.class, () -> updateService.update("foo", "bar"));
        }, executor);
        CompletableFuture.allOf(future1, future2).join();
    }

    @Test
    public void testUpdateAllWait() throws InterruptedException {
        var namespaceName = "foo";
        var extensionName = "bar";
        Mockito.when(repositories.findPublicId(namespaceName, extensionName)).thenAnswer(invocation -> {
            Thread.sleep(20000);

            var namespace = new Namespace();
            namespace.setId(1L);
            namespace.setName(namespaceName);

            var extension = new Extension();
            extension.setId(2L);
            extension.setName(extensionName);
            extension.setNamespace(namespace);
            return extension;
        });

        var executor = Executors.newFixedThreadPool(2);
        var future1 = CompletableFuture.runAsync(() -> {
            try {
                updateService.update(namespaceName, extensionName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
        var future2 = CompletableFuture.runAsync(() -> {
            try {
                updateService.updateAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);
        CompletableFuture.allOf(future1, future2).join();

        var order = Mockito.inOrder(repositories);
        order.verify(repositories).findPublicId(namespaceName, extensionName);
        order.verify(repositories).findAllPublicIds();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        VSCodeIdUpdateService vsCodeIdUpdateService() {
            return new VSCodeIdUpdateService();
        }
    }
}
