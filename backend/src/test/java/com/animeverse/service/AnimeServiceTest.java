package com.animeverse.service;

import com.animeverse.dto.AnimeSaveRequest;
import com.animeverse.entity.Anime;
import com.animeverse.entity.AnimeStudio;
import com.animeverse.mapper.AnimeEpisodeMapper;
import com.animeverse.mapper.AnimeGenreMapper;
import com.animeverse.mapper.AnimeGenreRelationMapper;
import com.animeverse.mapper.AnimeMapper;
import com.animeverse.mapper.AnimePlatformRelationMapper;
import com.animeverse.mapper.AnimeReviewMapper;
import com.animeverse.mapper.AnimeStaffMapper;
import com.animeverse.mapper.AnimeStaffRelationMapper;
import com.animeverse.mapper.LabMapper;
import com.animeverse.mapper.StreamingPlatformMapper;
import com.animeverse.mapper.UserFavoriteMapper;
import com.animeverse.mapper.UserWatchHistoryMapper;
import com.animeverse.vo.AnimeDetailVO;
import com.animeverse.vo.AnimeListVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

    @Mock private AnimeMapper animeMapper;
    @Mock private AnimeEpisodeMapper episodeMapper;
    @Mock private AnimeGenreMapper genreMapper;
    @Mock private AnimeGenreRelationMapper genreRelationMapper;
    @Mock private AnimeStaffMapper staffMapper;
    @Mock private AnimeStaffRelationMapper staffRelationMapper;
    @Mock private StreamingPlatformMapper platformMapper;
    @Mock private AnimePlatformRelationMapper platformRelationMapper;
    @Mock private AnimeReviewMapper reviewMapper;
    @Mock private UserFavoriteMapper favoriteMapper;
    @Mock private UserWatchHistoryMapper watchHistoryMapper;
    @Mock private StudioService studioService;
    @Mock private LabMapper labMapper;

    @InjectMocks
    private AnimeService animeService;

    @Test
    void create_mapsLegacyContractNamesToActualSchemaColumns() {
        when(studioService.requireStudio(3L)).thenReturn(
                AnimeStudio.builder().id(3L).studioName("MAPPA").build()
        );
        when(animeMapper.insert(any(Anime.class))).thenAnswer(invocation -> {
            Anime anime = invocation.getArgument(0);
            anime.setId(55L);
            return 1;
        });
        when(animeMapper.selectAnimeView(55L, null)).thenReturn(detailRow());
        when(animeMapper.selectById(55L)).thenReturn(
                Anime.builder().id(55L).updatedAt(LocalDateTime.of(2026, 11, 16, 15, 0)).build()
        );
        when(genreRelationMapper.selectGenresByAnimeId(55L)).thenReturn(List.of());
        when(staffRelationMapper.selectStaffByAnimeId(55L)).thenReturn(List.of());
        when(platformRelationMapper.selectPlatformsByAnimeId(55L)).thenReturn(List.of());

        AnimeSaveRequest request = new AnimeSaveRequest(
                "测试动画",
                "テストアニメ",
                null,
                "Test Anime",
                3L,
                "https://example.test/cover.jpg",
                "简介",
                2024,
                null,
                null,
                "ONGOING",
                null,
                null,
                null
        );

        AnimeDetailVO result = animeService.create(request);

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(animeMapper).insert(captor.capture());
        Anime inserted = captor.getValue();
        assertThat(inserted.getTitleJp()).isEqualTo("テストアニメ");
        assertThat(inserted.getTitleEn()).isEqualTo("Test Anime");
        assertThat(inserted.getReleaseDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(inserted.getStatus()).isEqualTo("AIRING");
        assertThat(inserted.getAverageRating()).isNull();
        assertThat(inserted.getEpisodeCount()).isNull();
        assertThat(result.originalTitle()).isEqualTo("テストアニメ");
    }

    private AnimeListVO detailRow() {
        AnimeListVO row = new AnimeListVO();
        row.setId(55L);
        row.setTitle("测试动画");
        row.setTitleJp("テストアニメ");
        row.setOriginalTitle("テストアニメ");
        row.setTitleEn("Test Anime");
        row.setStudioId(3L);
        row.setStudioName("MAPPA");
        row.setReleaseDate(LocalDate.of(2024, 1, 1));
        row.setReleaseYear(2024);
        row.setStatus("AIRING");
        row.setEpisodeCount(12);
        row.setAverageRating(new BigDecimal("8.50"));
        row.setRating(new BigDecimal("8.50"));
        row.setRatingCount(0);
        row.setFavoriteCount(0L);
        row.setReviewCount(0L);
        row.setViewCount(0L);
        row.setFavorited(false);
        row.setCreatedAt(LocalDateTime.of(2026, 11, 16, 14, 0));
        return row;
    }
}
