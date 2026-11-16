package com.animeverse.service;

import com.animeverse.entity.OperationLogEntity;
import com.animeverse.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogService operationLogService;

    @Test
    void record_mapsAuditFieldsToActualOperationLogSchema() {
        operationLogService.record(
                7L,
                42L,
                "ANIME",
                "CREATE",
                "POST",
                "/api/admin/anime",
                "127.0.0.1",
                1,
                null
        );

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLogEntity log = captor.getValue();
        assertThat(log.getOperatorUserId()).isEqualTo(7L);
        assertThat(log.getActionType()).isEqualTo("ANIME_CREATE");
        assertThat(log.getTargetType()).isEqualTo("ANIME");
        assertThat(log.getTargetId()).isEqualTo(42L);
        assertThat(log.getDescription()).isEqualTo(
                "POST /api/admin/anime status=1"
        );
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
    }
}
