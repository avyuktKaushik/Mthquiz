package com.example.mathquiz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates random math questions. Every answer is a whole number.
 * Mixes plain arithmetic (order of operations) with "Find x" algebra.
 */
public class QuestionGenerator {

    private int scale = 2;          // 1 = easy, 2 = normal, 3 = hard
    private int algebraChance = 30; // percent of questions that are algebra

    public void setDifficulty(String difficulty) {
        scale = switch (difficulty.toLowerCase()) {
            case "easy" -> 1;
            case "hard" -> 3;
            default -> 2;
        };
    }

    public void setAlgebraChance(int percent) {
        algebraChance = Math.max(0, Math.min(100, percent));
    }

    private int r(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public Question generate() {
        return r(1, 100) <= algebraChance ? algebra() : arithmetic();
    }

    /** A random divisor of n (n must be composite), never 1 and never n itself. */
    private int divisorOf(int n) {
        List<Integer> divs = new ArrayList<>();
        for (int d = 2; d <= n / 2; d++) if (n % d == 0) divs.add(d);
        return divs.isEmpty() ? n : divs.get(r(0, divs.size() - 1));
    }

    // ---------------------------------------------------------------- arithmetic

    private Question arithmetic() {
        int s = scale;
        String P = "Solve";
        return switch (r(0, 12)) {
            case 0 -> {
                int a = r(10, 50 * s), b = r(10, 50 * s);
                yield new Question(P, a + " + " + b, a + b);
            }
            case 1 -> {
                int a = r(30, 60 * s + 30), b = r(10, a - 1);
                yield new Question(P, a + " − " + b, a - b);
            }
            case 2 -> {
                int a = r(2, 8 + 4 * s), b = r(2, 8 + 2 * s);
                yield new Question(P, a + " × " + b, (long) a * b);
            }
            case 3 -> {
                int b = r(2, 9 + s), q = r(2, 10 + 5 * s);
                yield new Question(P, (b * q) + " ÷ " + b, q);
            }
            case 4 -> { // a × b ÷ c + d   (like 2 × 5 ÷ 10 + 31)
                int a = r(2, 6 + 3 * s), b = r(2, 6 + 3 * s);
                int c = divisorOf(a * b), d = r(2, 25 * s);
                yield new Question(P, a + " × " + b + " ÷ " + c + " + " + d, (long) a * b / c + d);
            }
            case 5 -> { // a + b × c
                int a = r(2, 20 * s), b = r(2, 5 + 3 * s), c = r(2, 5 + 3 * s);
                yield new Question(P, a + " + " + b + " × " + c, a + (long) b * c);
            }
            case 6 -> { // a × b − c
                int a = r(2, 5 + 3 * s), b = r(2, 5 + 3 * s), c = r(1, a * b - 1);
                yield new Question(P, a + " × " + b + " − " + c, (long) a * b - c);
            }
            case 7 -> { // a − b × c
                int b = r(2, 5 + s), c = r(2, 5 + s), a = b * c + r(1, 20 * s);
                yield new Question(P, a + " − " + b + " × " + c, (long) a - (long) b * c);
            }
            case 8 -> { // (a + b) × c − d
                int a = r(2, 10 * s), b = r(2, 10 * s), c = r(2, 5 + s);
                int d = r(1, (a + b) * c - 1);
                yield new Question(P, "(" + a + " + " + b + ") × " + c + " − " + d, (long) (a + b) * c - d);
            }
            case 9 -> { // a × b + c × d
                int a = r(2, 5 + 3 * s), b = r(2, 5 + 3 * s), c = r(2, 5 + 3 * s), d = r(2, 5 + 3 * s);
                yield new Question(P, a + " × " + b + " + " + c + " × " + d, (long) a * b + (long) c * d);
            }
            case 10 -> { // a ÷ b + c × d
                int b = r(2, 9), q = r(2, 8 + 3 * s), c = r(2, 5 + s), d = r(2, 5 + s);
                yield new Question(P, (b * q) + " ÷ " + b + " + " + c + " × " + d, q + (long) c * d);
            }
            case 11 -> {
                int[] pcts = {10, 20, 25, 50, 75};
                int p = pcts[r(0, pcts.length - 1)];
                int base = 20 * r(1, 5 * s + 5);
                yield new Question(P, "What is " + p + "% of " + base + " ?", base * p / 100);
            }
            default -> { // a + b − c
                int a = r(10, 40 * s), b = r(10, 40 * s), c = r(1, a + b - 1);
                yield new Question(P, a + " + " + b + " − " + c, (long) a + b - c);
            }
        };
    }

    // ------------------------------------------------------------------- algebra

    private Question algebra() {
        int s = scale;
        return switch (r(0, 4)) {
            case 0 -> {
                int a = r(2, 5 + 2 * s), x = r(1, 10 + 5 * s), b = r(1, 20 * s);
                yield new Question("Find x if", a + "x + " + b + " = " + (a * x + b), x);
            }
            case 1 -> {
                int a = r(2, 5 + 2 * s), x = r(2, 10 + 5 * s), b = r(1, a * x - 1);
                yield new Question("Find x if", a + "x − " + b + " = " + (a * x - b), x);
            }
            case 2 -> {
                int a = r(2, 9), x = a * r(1, 10 + 3 * s), b = r(1, 15 * s);
                yield new Question("Find x if", "x ÷ " + a + " + " + b + " = " + (x / a + b), x);
            }
            case 3 -> {
                int x = r(2, 10 + 3 * s);
                yield new Question("Find x if (x > 0)", "x² = " + (x * x), x);
            }
            default -> {
                int c = r(1, 4), a = c + r(1, 5), x = r(1, 8 + 4 * s), b = r(1, 15 * s);
                int d = b + (a - c) * x;
                yield new Question("Find x if", a + "x + " + b + " = " + c + "x + " + d, x);
            }
        };
    }
}
