package br.edu.uniamerica.parceiro_auto.util;

public class CnpjValidator {

    private CnpjValidator() {
        // Impede a criação de objetos dessa classe.
    }

    public static boolean isValid(String cnpj) {

        if (cnpj == null) {
            return false;
        }

        String normalizedCnpj = cnpj.replaceAll("[^0-9]", "");

        if (normalizedCnpj.length() != 14) {
            return false;
        }

        // Impede CNPJs formados apenas pelo mesmo dígito.
        if (normalizedCnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        int firstDigit = calculateDigit(normalizedCnpj, 12);
        int secondDigit = calculateDigit(
                normalizedCnpj.substring(0, 12) + firstDigit,
                13
        );

        return normalizedCnpj.charAt(12) - '0' == firstDigit
                && normalizedCnpj.charAt(13) - '0' == secondDigit;
    }

    private static int calculateDigit(String cnpj, int length) {

        int sum = 0;
        int weight = length - 7;

        for (int i = 0; i < length; i++) {

            sum += (cnpj.charAt(i) - '0') * weight;

            weight--;

            if (weight < 2) {
                weight = 9;
            }
        }

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }
}