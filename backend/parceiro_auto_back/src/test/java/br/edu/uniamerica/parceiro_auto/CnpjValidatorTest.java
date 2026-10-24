package br.edu.uniamerica.parceiro_auto;

import br.edu.uniamerica.parceiro_auto.util.CnpjValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CnpjValidatorTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "123", "00000000000000", "11111111111111", "11222333000191", "11222333000182"})
    void rejectsMissingRepeatedOrIncorrectCheckDigits(String cnpj) {
        // Act
        boolean valid = CnpjValidator.isValid(cnpj);

        // Assert
        assertThat(valid).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "11.222.333/0001-81", "00000000000191"})
    void acceptsValidCnpjWithOrWithoutFormatting(String cnpj) {
        // Act
        boolean valid = CnpjValidator.isValid(cnpj);

        // Assert
        assertThat(valid).isTrue();
    }
}
