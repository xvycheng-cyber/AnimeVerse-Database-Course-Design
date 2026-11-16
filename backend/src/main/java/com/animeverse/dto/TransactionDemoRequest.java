package com.animeverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransactionDemoRequest(
        @NotBlank
        @Pattern(regexp = "^(commit|rollback|exception)$")
        String mode
) {
}
