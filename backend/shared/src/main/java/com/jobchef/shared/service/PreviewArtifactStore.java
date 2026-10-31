package com.jobchef.shared.service;

public interface PreviewArtifactStore {
    void put(String objectKey, byte[] content);

    byte[] get(String objectKey);

    void delete(String objectKey);
}
