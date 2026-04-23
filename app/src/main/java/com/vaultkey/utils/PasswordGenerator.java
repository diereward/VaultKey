package com.vaultkey.utils;

import android.content.Context;
import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.vaultkey.R;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,./<>?";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Zxcvbn ZXCVBN = new Zxcvbn();

    private static final Map<String, String> FEEDBACK_RU = new HashMap<>();
    static {
        FEEDBACK_RU.put("Use a few words, avoid common phrases", "Используйте несколько слов, избегайте распространённых фраз");
        FEEDBACK_RU.put("No need for symbols, digits, or uppercase letters", "Не обязательно использовать символы, цифры и заглавные буквы");
        FEEDBACK_RU.put("Add another word or two. Uncommon words are better.", "Добавьте ещё слово-два. Редкие слова лучше.");
        FEEDBACK_RU.put("Straight rows of keys are easy to guess", "Последовательности клавиш легко угадать");
        FEEDBACK_RU.put("Short keyboard patterns are easy to guess", "Короткие паттерны клавиатуры легко угадать");
        FEEDBACK_RU.put("Use a longer keyboard pattern with more turns", "Используйте более длинный паттерн с поворотами");
        FEEDBACK_RU.put("Repeats like \"aaa\" are easy to guess", "Повторы типа «aaa» легко угадать");
        FEEDBACK_RU.put("Sequences like abc or 6543 are easy to guess", "Последовательности типа abc или 6543 легко угадать");
        FEEDBACK_RU.put("Recent years are easy to guess", "Недавние годы легко угадать");
        FEEDBACK_RU.put("This is a top-10 common password", "Один из 10 самых распространённых паролей");
        FEEDBACK_RU.put("This is a top-100 common password", "Один из 100 самых распространённых паролей");
        FEEDBACK_RU.put("This is a very common password", "Очень распространённый пароль");
        FEEDBACK_RU.put("A word by itself is easy to guess", "Одно слово легко угадать");
    }

    private PasswordGenerator() {}

    public static String generate(int length, boolean lower, boolean upper,
                                  boolean digits, boolean symbols) {
        StringBuilder charset = new StringBuilder();
        if (lower) charset.append(LOWER);
        if (upper) charset.append(UPPER);
        if (digits) charset.append(DIGITS);
        if (symbols) charset.append(SYMBOLS);
        if (charset.length() == 0) charset.append(LOWER).append(DIGITS);

        String pool = charset.toString();

        StringBuilder result = new StringBuilder();
        if (lower && length > 0) result.append(pick(LOWER));
        if (upper && result.length() < length) result.append(pick(UPPER));
        if (digits && result.length() < length) result.append(pick(DIGITS));
        if (symbols && result.length() < length) result.append(pick(SYMBOLS));

        while (result.length() < length) result.append(pool.charAt(RANDOM.nextInt(pool.length())));
        return shuffle(result.toString());
    }

    public static PasswordAnalysis analyze(Context context, String password) {
        PasswordAnalysis a = new PasswordAnalysis();
        if (password == null || password.isEmpty()) return a;

        Strength strength = ZXCVBN.measure(password);
        a.score = strength.getScore();
        a.strength = strengthLabel(a.score);
        a.crackTime = crackTimeLabel(context, a.score);

        Feedback feedback = strength.getFeedback();
        if (feedback != null) {
            String warning = feedback.getWarning();
            if (warning != null && !warning.isEmpty()) a.issues.add(tr(warning));
            for (String s : feedback.getSuggestions()) a.suggestions.add(tr(s));
        }

        if (a.issues.isEmpty() && password.length() < 12 && context != null)
            a.issues.add(context.getString(R.string.generator_issue_too_short));

        int targetLen = Math.max(password.length() + 4, 16);
        for (int i = 0; i < 3; i++)
            a.alternatives.add(generate(targetLen, true, true, true, true));

        return a;
    }

    private static char pick(String from) { return from.charAt(RANDOM.nextInt(from.length())); }

    private static String shuffle(String input) {
        List<Character> chars = new ArrayList<>();
        for (char c : input.toCharArray()) chars.add(c);
        Collections.shuffle(chars, RANDOM);
        StringBuilder out = new StringBuilder(chars.size());
        for (char c : chars) out.append(c);
        return out.toString();
    }

    private static String strengthLabel(int score) {
        switch (score) {
            case 4: return "VERY_STRONG";
            case 3: return "STRONG";
            case 2: return "FAIR";
            case 1: return "WEAK";
            default: return "VERY_WEAK";
        }
    }

    private static String crackTimeLabel(Context ctx, int score) {
        if (ctx == null) {
            switch (score) { case 4: return "centuries"; case 3: return "years"; case 2: return "hours"; case 1: return "minutes"; default: return "seconds"; }
        }
        switch (score) { case 4: return ctx.getString(R.string.crack_time_centuries); case 3: return ctx.getString(R.string.crack_time_years); case 2: return ctx.getString(R.string.crack_time_hours); case 1: return ctx.getString(R.string.crack_time_minutes); default: return ctx.getString(R.string.crack_time_seconds); }
    }

    private static String tr(String text) { String t = FEEDBACK_RU.get(text); return t == null ? text : t; }

    public static class PasswordAnalysis {
        public String strength = "VERY_WEAK";
        public int score = 0;
        public String crackTime = "";
        public List<String> issues = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
        public List<String> alternatives = new ArrayList<>();
    }
}
