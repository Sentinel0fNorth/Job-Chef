package com.jobchef.api.web;

import com.jobchef.shared.api.ApiModels.ApplicationResponse;
import com.jobchef.shared.service.ApplicationService;
import com.jobchef.shared.service.PreviewArtifactStore;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationService applications;
    private final PreviewArtifactStore artifacts;

    public ApplicationController(ApplicationService applications, PreviewArtifactStore artifacts) {
        this.applications = applications;
        this.artifacts = artifacts;
    }

    @GetMapping("/by-job/{jobId}")
    ApplicationResponse byJob(@PathVariable UUID jobId, HttpServletRequest request) {
        return applications.byJob(RequestContext.device(request).deviceId(), jobId);
    }

    @GetMapping(value = "/by-job/{jobId}/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> preview(@PathVariable UUID jobId, HttpServletRequest request) {
        UUID deviceId = RequestContext.device(request).deviceId();
        String objectKey = applications.previewObjectKey(deviceId, jobId);
        byte[] pdf = artifacts.get(objectKey);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("job-chef-sanitized-preview.pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }

    @GetMapping("/{applicationId}")
    ApplicationResponse byId(@PathVariable UUID applicationId, HttpServletRequest request) {
        return applications.byId(RequestContext.device(request).deviceId(), applicationId);
    }

    @PostMapping("/by-job/{jobId}/regenerate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void regenerate(@PathVariable UUID jobId, HttpServletRequest request) {
        var deviceId = RequestContext.device(request).deviceId();
        applications.regenerate(deviceId, jobId);
    }

    @DeleteMapping("/by-job/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID jobId, HttpServletRequest request) {
        applications.delete(RequestContext.device(request).deviceId(), jobId);
    }
}
