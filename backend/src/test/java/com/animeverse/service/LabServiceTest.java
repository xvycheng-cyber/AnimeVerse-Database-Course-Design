package com.animeverse.service;

import com.animeverse.dto.AddAnimeProcedureRequest;
import com.animeverse.mapper.AnimeMapper;
import com.animeverse.mapper.LabMapper;
import com.animeverse.mapper.UserFavoriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Types;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock private LabMapper labMapper;
    @Mock private AnimeMapper animeMapper;
    @Mock private UserFavoriteMapper favoriteMapper;
    @Mock private FavoriteService favoriteService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;

    @Test
    void addAnime_usesEightInputParametersAndOneOutputParameter() throws Exception {
        doAnswer(invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        }).when(jdbcTemplate).execute(any(ConnectionCallback.class));
        when(connection.prepareCall("{call sp_add_anime(?, ?, ?, ?, ?, ?, ?, ?, ?)}"))
                .thenReturn(statement);
        when(statement.getLong(9)).thenReturn(77L);

        LabService service = new LabService(
                labMapper,
                animeMapper,
                favoriteMapper,
                favoriteService,
                jdbcTemplate
        );
        AddAnimeProcedureRequest request = new AddAnimeProcedureRequest(
                "测试动画",
                "テストアニメ",
                null,
                "Test Anime",
                3L,
                "https://example.test/cover.jpg",
                "简介",
                null,
                2024,
                null,
                "ONGOING",
                null
        );

        Map<String, Object> result = service.addAnime(request);

        verify(statement).setString(1, "测试动画");
        verify(statement).setString(2, "テストアニメ");
        verify(statement).setString(3, "Test Anime");
        verify(statement).setLong(4, 3L);
        verify(statement).setString(5, "简介");
        verify(statement).setString(6, "https://example.test/cover.jpg");
        verify(statement).setDate(7, Date.valueOf("2024-01-01"));
        verify(statement).setString(8, "AIRING");
        verify(statement).registerOutParameter(9, Types.BIGINT);
        verify(statement).execute();
        assertThat(result).containsEntry("animeId", 77L);
        assertThat(result).containsEntry("source", "sp_add_anime");
    }

    @Test
    void deleteAnime_usesProcedureAndReturnsAffectedRowCount() throws Exception {
        doAnswer(invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        }).when(jdbcTemplate).execute(any(ConnectionCallback.class));
        when(connection.prepareCall("{call sp_delete_anime(?, ?)}")).thenReturn(statement);
        when(statement.getInt(2)).thenReturn(1);

        LabService service = new LabService(
                labMapper,
                animeMapper,
                favoriteMapper,
                favoriteService,
                jdbcTemplate
        );

        Map<String, Object> result = service.deleteAnime(77L);

        verify(statement).setLong(1, 77L);
        verify(statement).registerOutParameter(2, Types.INTEGER);
        verify(statement).execute();
        assertThat(result).containsEntry("animeId", 77L);
        assertThat(result).containsEntry("deletedRows", 1);
        assertThat(result).containsEntry("source", "sp_delete_anime");
    }
}
