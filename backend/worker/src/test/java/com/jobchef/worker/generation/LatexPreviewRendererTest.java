package com.jobchef.worker.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LatexPreviewRendererTest {
    @Test
    void escapesLatexControlCharactersAsText() {
        String escaped = LatexPreviewRenderer.escape(
                "\\input{secret} # $ % & _ ^ ~\nsecond line");

        assertTrue(escaped.contains("\\textbackslash{}input\\{secret\\}"));
        assertTrue(escaped.contains("\\# \\$ \\% \\& \\_ \\textasciicircum{} \\textasciitilde{}"));
        assertFalse(escaped.contains("\\input{secret}"));
    }

    @Test
    void documentTreatsGeneratedFieldsAsEscapedText() {
        var resume = new GeminiClient.GeneratedResume(
                "Engineer", "Uses \\write18{bad} • builds resilient systems — with care",
                List.of("Java_21"),
                List.of(new GeminiClient.Experience(
                        "Example", "Developer", "2024", List.of("Built #1 system"))),
                List.of(new GeminiClient.Education("BSc", "Example University", "2020")));

        String tex = LatexPreviewRenderer.latexDocument(
                new GeminiClient.GeneratedApplication(resume, "Cover letter 100% safe"));

        assertTrue(tex.contains("Uses \\textbackslash{}write18\\{bad\\}"));
        assertTrue(tex.contains("Java\\_21"));
        assertTrue(tex.contains("\\textbullet{} builds resilient systems --- with care"));
        assertTrue(tex.contains("Cover letter 100\\% safe"));
        assertFalse(tex.contains("\\write18{bad}"));
        assertTrue(tex.contains("\\end{document}"));
    }
}
