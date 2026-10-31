package com.jobchef.shared.service;

import java.util.regex.Pattern;

public final class ResumePrivacyGuard {
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern INTERNATIONAL_PHONE = Pattern.compile(
            "(?<!\\d)\\+\\d{1,3}(?:[\\s().-]?\\d){7,13}(?!\\d)");
    private static final Pattern INDIAN_PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?91[\\s.-]?)?[6-9]\\d{9}(?!\\d)");
    private static final Pattern US_PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?1[\\s.-]?)?(?:\\(?[2-9]\\d{2}\\)?[\\s.-]?)\\d{3}[\\s.-]?\\d{4}(?!\\d)");
    private static final Pattern LABELED_ADDRESS = Pattern.compile(
            "(?im)^\\s*(?:address|location)\\s*:\\s*(?!\\[CONTACT REDACTED\\]).+");

    private ResumePrivacyGuard() {}

    public static boolean containsObviousContactData(String text) {
        if (text == null) return false;
        return EMAIL.matcher(text).find()
                || INTERNATIONAL_PHONE.matcher(text).find()
                || INDIAN_PHONE.matcher(text).find()
                || US_PHONE.matcher(text).find()
                || LABELED_ADDRESS.matcher(text).find();
    }
}
