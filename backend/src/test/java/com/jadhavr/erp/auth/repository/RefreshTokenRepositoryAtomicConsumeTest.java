package com.jadhavr.erp.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryAtomicConsumeTest {
    @Test
    void refreshConsumptionIsAConditionalDatabaseUpdate() throws Exception {
        var method = RefreshTokenRepository.class.getMethod("consume", String.class);
        Modifying modifying = method.getAnnotation(Modifying.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(modifying).isNotNull();
        assertThat(query).isNotNull();
        assertThat(query.value()).contains("t.revoked = false");
    }
}
