package com.jobchef.worker.generation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LatexPreviewRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(LatexPreviewRenderer.class);
    private static final Duration COMPILE_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_PDF_BYTES = 10 * 1024 * 1024;
    private final String compiler;

    public LatexPreviewRenderer(@Value("${jobchef.artifacts.latex-executable:pdflatex}") String compiler) {
        this.compiler = compiler;
    }

    public byte[] render(GeminiClient.GeneratedApplication application) {
        Path directory;
        try {
            directory = Files.createTempDirectory("jobchef-preview-");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not allocate a private PDF build directory.", exception);
        }
        try {
            Path source = directory.resolve("preview.tex");
            Path output = directory.resolve("preview.pdf");
            Path compilerLog = directory.resolve("compiler.log");
            Files.writeString(source, latexDocument(application), StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(
                    compiler,
                    "-no-shell-escape",
                    "-interaction=nonstopmode",
                    "-halt-on-error",
                    "-output-directory=" + directory,
                    source.toString())
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(compilerLog.toFile())
                    .start();
            boolean completed;
            try {
                completed = process.waitFor(COMPILE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("PDF compilation was interrupted.", exception);
            }
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("PDF compilation exceeded its time limit.");
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
                throw new IllegalStateException("The sanitized PDF preview could not be compiled.");
            }
            long size = Files.size(output);
            if (size < 5 || size > MAX_PDF_BYTES) {
                throw new IllegalStateException("The sanitized PDF preview exceeded its size limit.");
            }
            byte[] pdf = Files.readAllBytes(output);
            if (pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F' || pdf[4] != '-') {
                throw new IllegalStateException("The PDF compiler returned an invalid document.");
            }
            return pdf;
        } catch (IOException exception) {
            throw new IllegalStateException("The sanitized PDF preview could not be created.", exception);
        } finally {
            removeTemporaryDirectory(directory);
        }
    }

    static String latexDocument(GeminiClient.GeneratedApplication application) {
        StringBuilder tex = new StringBuilder("""
                \\documentclass[11pt]{article}
                \\usepackage[margin=0.75in]{geometry}
                \\usepackage[T1]{fontenc}
                \\usepackage[utf8]{inputenc}
                \\usepackage{lmodern}
                \\usepackage{textcomp}
                \\setlength{\\parindent}{0pt}
                \\setlength{\\parskip}{5pt}
                \\begin{document}
                """);
        GeminiClient.GeneratedResume resume = application.resumeContent();
        if (resume != null) {
            heading(tex, resume.headline());
            paragraph(tex, resume.summary());
            section(tex, "Skills");
            bullets(tex, resume.skills());
            section(tex, "Experience");
            if (resume.experience() != null) {
                for (GeminiClient.Experience experience : resume.experience()) {
                    if (experience == null) continue;
                    heading(tex, join(experience.role(), experience.employer()));
                    paragraph(tex, experience.dates());
                    bullets(tex, experience.bullets());
                }
            }
            section(tex, "Education");
            if (resume.education() != null) {
                for (GeminiClient.Education education : resume.education()) {
                    if (education == null) continue;
                    heading(tex, education.qualification());
                    paragraph(tex, join(education.institution(), education.dates()));
                }
            }
        }
        section(tex, "Cover Letter");
        paragraph(tex, application.coverLetter());
        tex.append("\\end{document}\n");
        return tex.toString();
    }

    static String escape(String text) {
        if (text == null) return "";
        StringBuilder escaped = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            switch (codePoint) {
                case '\\' -> escaped.append("\\textbackslash{}");
                case '{' -> escaped.append("\\{");
                case '}' -> escaped.append("\\}");
                case '#' -> escaped.append("\\#");
                case '$' -> escaped.append("\\$");
                case '%' -> escaped.append("\\%");
                case '&' -> escaped.append("\\&");
                case '_' -> escaped.append("\\_");
                case '^' -> escaped.append("\\textasciicircum{}");
                case '~' -> escaped.append("\\textasciitilde{}");
                case 0x00A0 -> escaped.append('~');
                case 0x2013 -> escaped.append("--");
                case 0x2014 -> escaped.append("---");
                case 0x2018, 0x2019 -> escaped.append('\'');
                case 0x201C -> escaped.append("``");
                case 0x201D -> escaped.append("''");
                case 0x2022 -> escaped.append("\\textbullet{}");
                case '\n' -> escaped.append("\\par\n");
                case '\r' -> { }
                case '\t' -> escaped.append("  ");
                default -> {
                    if (!Character.isISOControl(codePoint)) escaped.appendCodePoint(codePoint);
                }
            }
        });
        return escaped.toString();
    }

    private static void heading(StringBuilder tex, String text) {
        if (text != null && !text.isBlank()) {
            tex.append("{\\Large\\textbf{").append(escape(text)).append("}}\\par\n");
        }
    }

    private static void section(StringBuilder tex, String title) {
        tex.append("\\vspace{4pt}\\section*{").append(escape(title)).append("}\n");
    }

    private static void paragraph(StringBuilder tex, String text) {
        if (text != null && !text.isBlank()) tex.append(escape(text)).append("\\par\n");
    }

    private static void bullets(StringBuilder tex, List<String> items) {
        if (items == null || items.isEmpty()) return;
        tex.append("\\begin{itemize}\n");
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                tex.append("\\item ").append(escape(item)).append('\n');
            }
        }
        tex.append("\\end{itemize}\n");
    }

    private static String join(String first, String second) {
        if (first == null || first.isBlank()) return second == null ? "" : second;
        if (second == null || second.isBlank()) return first;
        return first + " - " + second;
    }

    private static void removeTemporaryDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    LOG.warn("Could not remove temporary PDF build file {}.", path);
                }
            });
        } catch (IOException exception) {
            LOG.warn("Could not clean up temporary PDF build directory {}.", directory);
        }
    }
}
