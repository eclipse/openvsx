package org.eclipse.openvsx.dto;

public class NamespaceDTO {

    private final long id;
    private final String publicId;
    private final String name;

    public NamespaceDTO(long id, String publicId, String name) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }
}
