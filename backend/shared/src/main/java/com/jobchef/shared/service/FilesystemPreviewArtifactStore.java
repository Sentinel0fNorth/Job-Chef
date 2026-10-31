package com.jobchef.shared.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "local"})
public class FilesystemPreviewArtifactStore implements PreviewArtifactStore {
    private static final long MAX_ARTIFACT_BYTES = 10L * 1024 * 1024;
    private final Path root;

    public FilesystemPreviewArtifactStore(
            @Value("${jobchef.artifacts.local-directory:./.jobchef/artifacts}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void put(String objectKey, byte[] content) {
        if (content == null || content.length > MAX_ARTIFACT_BYTES) {
            throw new IllegalArgumentException("Preview artifact exceeds the supported size limit.");
        }
        Path destination = resolve(objectKey);
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            Files.write(temporary, content);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store the private preview artifact.", exception);
        }
    }

    @Override
    public byte[] get(String objectKey) {
        try {
            Path file = resolve(objectKey);
            if (Files.size(file) > MAX_ARTIFACT_BYTES) {
                throw new IllegalStateException("The private preview artifact exceeds the supported size limit.");
            }
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new IllegalStateException("The private preview artifact is unavailable.", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete the private preview artifact.", exception);
        }
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || !objectKey.matches("[A-Za-z0-9._/-]{1,240}")
                || objectKey.contains("..")) {
            throw new IllegalArgumentException("Invalid private artifact key.");
        }
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Artifact key escapes the private storage directory.");
        }
        return resolved;
    }
}
