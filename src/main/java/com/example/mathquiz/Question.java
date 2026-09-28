package com.example.mathquiz;

/** A single math question. */
public record Question(String prompt, String expression, long answer) {}
