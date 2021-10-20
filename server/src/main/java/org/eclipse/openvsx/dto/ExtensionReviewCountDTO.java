package org.eclipse.openvsx.dto;

public class ExtensionReviewCountDTO {

    private final long extensiondId;
    private final long reviewCount;

    public ExtensionReviewCountDTO(long extensiondId, long reviewCount) {
        this.extensiondId = extensiondId;
        this.reviewCount = reviewCount;
    }

    public long getExtensiondId() {
        return extensiondId;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
