package com.moyun.ledger.domain.dto;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 记账 DTO 校验契约：保证 @Valid 生效后，非法入参能在 Controller 前被拦截。
 */
class TransactionDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void minimalValidPayloadPasses() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType("expense");
        dto.setAmount(new BigDecimal("12.50"));
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void missingAmountRejected() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType("expense");
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getMessage().contains("金额")), "缺少金额应被拦截");
    }

    @Test
    void missingTypeRejected() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal("1.00"));
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getMessage().contains("记账类型")), "缺少类型应被拦截");
    }

    @Test
    void overlongDescriptionRejected() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType("expense");
        dto.setAmount(new BigDecimal("1.00"));
        dto.setDescription("x".repeat(201));
        assertEquals(1, validator.validate(dto).size());
    }

    @Test
    void badTimeFormatRejected() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType("expense");
        dto.setAmount(new BigDecimal("1.00"));
        dto.setTransactionTime("25:99");
        assertEquals(1, validator.validate(dto).size());
    }
}
