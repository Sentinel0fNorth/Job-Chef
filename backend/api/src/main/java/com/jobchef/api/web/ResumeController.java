package com.jobchef.api.web;

import com.jobchef.shared.api.ApiModels.ResumeRequest;
import com.jobchef.shared.api.ApiModels.ResumeResponse;
import com.jobchef.shared.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final ResumeService resumes;

    public ResumeController(ResumeService resumes) {
        this.resumes = resumes;
    }

    @GetMapping
    List<ResumeResponse> list(HttpServletRequest request) {
        return resumes.list(RequestContext.device(request).deviceId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResumeResponse create(@Valid @RequestBody ResumeRequest body, HttpServletRequest request) {
        return resumes.add(RequestContext.device(request).deviceId(), body);
    }

    @PutMapping("/{resumeId}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activate(@PathVariable UUID resumeId, HttpServletRequest request) {
        resumes.activate(RequestContext.device(request).deviceId(), resumeId);
    }

    @DeleteMapping("/{resumeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID resumeId, HttpServletRequest request) {
        resumes.delete(RequestContext.device(request).deviceId(), resumeId);
    }
}
