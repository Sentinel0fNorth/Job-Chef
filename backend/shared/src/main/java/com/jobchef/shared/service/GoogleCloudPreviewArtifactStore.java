package com.jobchef.shared.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live")
public class GoogleCloudPreviewArtifactStore implements PreviewArtifactStore {
    private static final long MAX_ARTIFACT_BYTES = 10L * 1024 * 1024;
    private final Storage storage;
    private final String bucket;

    public GoogleCloudPreviewArtifactStore(@Value("${jobchef.artifacts.bucket:}") String bucket) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("JOB_CHEF_ARTIFACTS_BUCKET must be configured in live mode.");
        }
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, byte[] content) {
        if (content == null || content.length > MAX_ARTIFACT_BYTES) {
            throw new IllegalArgumentException("Preview artifact exceeds the supported size limit.");
        }
        storage.create(BlobInfo.newBuilder(BlobId.of(bucket, validateKey(objectKey)))
                .setContentType("application/pdf")
                .setCacheControl("private, no-store")
                .build(), content);
    }

    @Override
    public byte[] get(String objectKey) {
        Blob blob = storage.get(BlobId.of(bucket, validateKey(objectKey)));
        if (blob == null) {
            throw new IllegalStateException("The private preview artifact is unavailable.");
        }
        if (blob.getSize() > MAX_ARTIFACT_BYTES) {
            throw new IllegalStateException("The private preview artifact exceeds the supported size limit.");
        }
        return blob.getContent();
    }

    @Override
    public void delete(String objectKey) {
        storage.delete(BlobId.of(bucket, validateKey(objectKey)));
    }

    private static String validateKey(String objectKey) {
        if (objectKey == null || !objectKey.matches("[A-Za-z0-9._/-]{1,240}")
                || objectKey.contains("..")) {
            throw new IllegalArgumentException("Invalid private artifact key.");
        }
        return objectKey;
    }
}
