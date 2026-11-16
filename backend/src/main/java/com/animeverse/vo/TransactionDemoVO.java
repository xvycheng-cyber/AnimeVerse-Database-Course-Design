package com.animeverse.vo;

public record TransactionDemoVO(
        String mode,
        String marker,
        boolean committed,
        boolean rolledBack,
        long visibleRows,
        String error
) {
}
