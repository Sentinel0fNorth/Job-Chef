package com.jobchef.shared.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import tools.jackson.databind.json.JsonMapper;
import com.jobchef.shared.api.ApiModels.DecisionRequest;
import com.jobchef.shared.config.BackendProperties;
import com.jobchef.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class DecisionSyncServiceTest {
    private final JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    private final DecisionSyncService service = new DecisionSyncService(
            jdbc,
            JsonMapper.builder().build(),
            new BackendProperties(new BackendProperties.Limits(
                    20, 5, 1, 20, 50000, 20000, 60000, 24000, 2, 10, 100000)));

    @Test
    void rejectsOversizedBatchBeforeDatabaseAccess() {
        List<DecisionRequest> decisions = java.util.Collections.nCopies(101, null);

        assertThrows(ApiException.class, () -> service.synchronize(UUID.randomUUID(), decisions));
        verifyNoInteractions(jdbc);
    }

    @Test
    void reportsMalformedBatchItemsWithoutFailingTheWholeSync() {
        var response = service.synchronize(UUID.randomUUID(), java.util.Arrays.asList((DecisionRequest) null));

        assertEquals(0, response.accepted());
        assertEquals(List.of("unknown:INVALID_DECISION"), response.errors());
        verifyNoInteractions(jdbc);
    }
}
