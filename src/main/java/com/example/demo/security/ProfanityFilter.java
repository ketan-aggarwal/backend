package com.example.demo.security;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProfanityFilter {
    // A list of common inappropriate words (mild examples for demonstration)
    private static final List<String> BANNED_WORDS = Arrays.asList(
        "fuck", "shit", "asshole", "bitch", "bastard", "crap", "spam", "scam", "idiot", "dumb"
    );

    public boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : BANNED_WORDS) {
            // Match whole words using word boundary matcher (\b) to avoid false positives (e.g. "class", "assess")
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
            if (pattern.matcher(lowerText).find()) {
                return true;
            }
        }
        return false;
    }

    public String censorText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        String censored = text;
        for (String word : BANNED_WORDS) {
            // Match whole words case-insensitively and replace with asterisks
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
            String replacement = "*".repeat(word.length());
            censored = pattern.matcher(censored).replaceAll(replacement);
        }
        return censored;
    }
}
