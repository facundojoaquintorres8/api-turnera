package com.f8.turnera.util;

import org.springframework.util.StringUtils;

import com.f8.turnera.exception.BadRequestException;

public class EmailValidation {

    @SuppressWarnings("null")
    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            executeException();
        }
        email = email.toLowerCase();

        String validCharacters = "abcdefghijklmnopqrstuvwxyz0123456789.-_%"; // TODO: ver caracters válidos
        String[] emailParts = email.split("@");
        if (emailParts.length != 2) {
            executeException();
        }
        String username = emailParts[0];
        String domain = emailParts[1];

        if (domain.startsWith(".") || domain.startsWith("-")
        || domain.startsWith("_") || domain.startsWith("%")
        || domain.endsWith(".") || domain.endsWith("-") || domain.endsWith("_")
        || domain.endsWith("%")
        || StringUtils.containsWhitespace(username) || username.isEmpty()
        || StringUtils.containsWhitespace(domain) || domain.isEmpty()) {
            executeException();
        }

        for (char item : username.toCharArray()) {
            if (validCharacters.indexOf(item) == -1) {
                executeException();
            }
        }

        for (char item : domain.toCharArray()) {
            if (validCharacters.indexOf(item) == -1) {
                executeException();
            }
        }

        if (domain.indexOf(".") == -1) {
            executeException();
        }

        // TODO: validar dos caracteres especiales seguidos (.. __ --)
    }

    private static void executeException() {
        throw new BadRequestException("El Correo Electrónico es inválido.");
    }
}
