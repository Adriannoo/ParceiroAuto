package br.edu.uniamerica.parceiro_auto.controller.dto;

import br.edu.uniamerica.parceiro_auto.entity.enums.RecurrenceFrequency;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecurrenceRuleRequestDTO(
        @NotNull(message = "A frequencia da recorrencia e obrigatoria")
        RecurrenceFrequency frequency,
        LocalDate endDate
) {}
