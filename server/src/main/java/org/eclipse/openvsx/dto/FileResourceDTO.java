package org.eclipse.openvsx.dto;

public class FileResourceDTO {

    private final long extensionVersionId;
    private ExtensionVersionDTO extensionVersion;

    private final long id;
    private final String name;
    private final String type;

    public FileResourceDTO(long id, long extensionVersionId, String name, String type) {
        this.id = id;
        this.extensionVersionId = extensionVersionId;
        this.name = name;
        this.type = type;
    }

    public long getExtensionVersionId() {
        return extensionVersionId;
    }

    public ExtensionVersionDTO getExtensionVersion() {
        return extensionVersion;
    }

    public void setExtensionVersion(ExtensionVersionDTO extensionVersion) {
        if(extensionVersion.getId() == extensionVersionId) {
            this.extensionVersion = extensionVersion;
        } else {
            throw new IllegalArgumentException("extensionVersion must have the same id as extensionVersionId");
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
