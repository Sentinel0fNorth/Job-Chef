package com.jobchef.shared.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemPreviewArtifactStoreTest {
    @TempDir
    java.nio.file.Path directory;

    @Test
    void storesAndDeletesPrivateArtifacts() {
        FilesystemPreviewArtifactStore store = new FilesystemPreviewArtifactStore(directory.toString());
        byte[] content = "%PDF-synthetic".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        store.put("applications/123/1/preview.pdf", content);
        assertArrayEquals(content, store.get("applications/123/1/preview.pdf"));

        store.delete("applications/123/1/preview.pdf");
        assertTrue(assertThrows(IllegalStateException.class,
                () -> store.get("applications/123/1/preview.pdf")).getMessage()
                .contains("unavailable"));
    }

    @Test
    void rejectsPathTraversalBeforeFilesystemAccess() {
        FilesystemPreviewArtifactStore store = new FilesystemPreviewArtifactStore(directory.toString());

        assertThrows(IllegalArgumentException.class, () -> store.get("../private.pdf"));
        assertTrue(Files.notExists(directory.resolve("private.pdf")));
    }
}
