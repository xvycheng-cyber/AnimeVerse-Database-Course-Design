package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.AnimeSaveRequest;
import com.animeverse.entity.Anime;
import com.animeverse.entity.AnimeEpisode;
import com.animeverse.entity.AnimeGenreRelation;
import com.animeverse.entity.AnimePlatformRelation;
import com.animeverse.entity.AnimeStaffRelation;
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
import com.animeverse.security.SecurityUtils;
import com.animeverse.vo.AnimeDetailVO;
import com.animeverse.vo.AnimeListVO;
import com.animeverse.vo.RankingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnimeService {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("latest", "rating", "releaseDate", "favorites", "views", "title", "popular", "score");
    private static final Set<String> ALLOWED_STAFF_ROLES = Set.of(
            "DIRECTOR", "SERIES_COMPOSITION", "MUSIC", "ORIGINAL_CREATOR",
            "CHARACTER_DESIGN", "VOICE_ACTOR", "PRODUCER", "ANIMATION_DIRECTOR"
    );

    private final AnimeMapper animeMapper;
    private final AnimeEpisodeMapper episodeMapper;
    private final AnimeGenreMapper genreMapper;
    private final AnimeGenreRelationMapper genreRelationMapper;
    private final AnimeStaffMapper staffMapper;
    private final AnimeStaffRelationMapper staffRelationMapper;
    private final StreamingPlatformMapper platformMapper;
    private final AnimePlatformRelationMapper platformRelationMapper;
    private final AnimeReviewMapper reviewMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final UserWatchHistoryMapper watchHistoryMapper;
    private final StudioService studioService;
    private final LabMapper labMapper;

    public PageResult<AnimeListVO> page(
            long page,
            long size,
            String keyword,
            Long genreId,
            Long studioId,
            String status,
            String sort,
            boolean publicOnly
    ) {
        String safeSort = normalizeSort(sort);
        String normalizedStatus = status == null || status.isBlank()
                ? null
                : normalizeStatus(status);
        IPage<AnimeListVO> result = animeMapper.selectAnimePage(
                Page.of(page, size),
                trimToNull(keyword),
                genreId,
                studioId,
                normalizedStatus,
                safeSort,
                publicOnly,
                SecurityUtils.currentUserIdOrNull()
        );
        return PageResult.of(result);
    }

    public AnimeDetailVO get(Long animeId) {
        AnimeListVO base = animeMapper.selectAnimeView(animeId, SecurityUtils.currentUserIdOrNull());
        if (base == null) {
            throw BusinessException.notFound("动漫不存在");
        }
        Anime anime = requireAnime(animeId);
        return new AnimeDetailVO(
                base.getId(),
                base.getTitle(),
                base.getTitleJp(),
                base.getTitleEn(),
                base.getOriginalTitle(),
                base.getStudioId(),
                base.getStudioName(),
                base.getCoverUrl(),
                base.getSynopsis(),
                base.getReleaseYear(),
                base.getReleaseDate(),
                base.getEndDate(),
                base.getStatus(),
                base.getEpisodeCount(),
                base.getAverageRating(),
                base.getRatingCount(),
                base.getRating(),
                base.getFavoriteCount(),
                base.getReviewCount(),
                base.getViewCount(),
                genreRelationMapper.selectGenresByAnimeId(animeId),
                staffRelationMapper.selectStaffByAnimeId(animeId),
                platformRelationMapper.selectPlatformsByAnimeId(animeId),
                base.getCreatedAt(),
                anime.getUpdatedAt(),
                base.getFavorited()
        );
    }

    public List<RankingVO> ranking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return labMapper.callAnimeRanking(safeLimit);
    }

    @Transactional
    public AnimeDetailVO create(AnimeSaveRequest request) {
        studioService.requireStudio(request.studioId());
        LocalDate releaseDate = resolveReleaseDate(request.releaseDate(), request.releaseYear());
        validateDates(releaseDate, request.endDate());

        LocalDateTime now = LocalDateTime.now();
        Anime anime = Anime.builder()
                .title(request.title().trim())
                .titleJp(firstNonBlank(request.titleJp(), request.originalTitle()))
                .titleEn(trimToNull(request.titleEn()))
                .studioId(request.studioId())
                .coverUrl(trimToNull(request.coverUrl()))
                .synopsis(trimToNull(request.synopsis()))
                .releaseDate(releaseDate)
                .endDate(request.endDate())
                .status(normalizeStatus(request.status() == null ? "UPCOMING" : request.status()))
                .ratingCount(0)
                .favoriteCount(0L)
                .reviewCount(0L)
                .viewCount(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
        animeMapper.insert(anime);
        replaceProvidedRelations(anime.getId(), request);
        return get(anime.getId());
    }

    @Transactional
    public AnimeDetailVO update(Long animeId, AnimeSaveRequest request) {
        Anime anime = requireAnime(animeId);
        studioService.requireStudio(request.studioId());

        LocalDate releaseDate = request.releaseDate() != null || request.releaseYear() != null
                ? resolveReleaseDate(request.releaseDate(), request.releaseYear())
                : anime.getReleaseDate();
        LocalDate endDate = request.endDate() == null ? anime.getEndDate() : request.endDate();
        validateDates(releaseDate, endDate);

        anime.setTitle(request.title().trim());
        anime.setTitleJp(firstNonBlank(request.titleJp(), request.originalTitle()));
        anime.setTitleEn(trimToNull(request.titleEn()));
        anime.setStudioId(request.studioId());
        anime.setCoverUrl(trimToNull(request.coverUrl()));
        anime.setSynopsis(trimToNull(request.synopsis()));
        anime.setReleaseDate(releaseDate);
        anime.setEndDate(endDate);
        if (request.status() != null) {
            anime.setStatus(normalizeStatus(request.status()));
        }
        anime.setUpdatedAt(LocalDateTime.now());
        animeMapper.updateById(anime);
        replaceProvidedRelations(animeId, request);
        return get(animeId);
    }

    @Transactional
    public void delete(Long animeId) {
        requireAnime(animeId);
        if (favoriteMapper.countByAnimeId(animeId) > 0) {
            throw BusinessException.conflict("动漫已有用户收藏，不能删除");
        }
        if (reviewMapper.countByAnimeId(animeId) > 0) {
            throw BusinessException.conflict("动漫已有用户评论，不能删除");
        }
        if (watchHistoryMapper.countByAnimeId(animeId) > 0) {
            throw BusinessException.conflict("动漫已有观看历史，不能删除");
        }
        if (episodeMapper.selectCount(
                new LambdaQueryWrapper<AnimeEpisode>().eq(AnimeEpisode::getAnimeId, animeId)
        ) > 0) {
            throw BusinessException.conflict("请先删除动漫剧集");
        }
        clearRelations(animeId);
        animeMapper.deleteById(animeId);
    }

    public Anime requireAnime(Long animeId) {
        Anime anime = animeMapper.selectById(animeId);
        if (anime == null) {
            throw BusinessException.notFound("动漫不存在");
        }
        return anime;
    }

    public static String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UPCOMING" -> "UPCOMING";
            case "AIRING", "ONGOING", "PAUSED" -> "AIRING";
            case "COMPLETED", "FINISHED" -> "COMPLETED";
            default -> throw BusinessException.badRequest("动漫状态不合法");
        };
    }

    private static String normalizeSort(String sort) {
        String requested = sort == null || sort.isBlank() ? "favorites" : sort.trim();
        if (!ALLOWED_SORTS.contains(requested)) {
            return "favorites";
        }
        return switch (requested) {
            case "popular" -> "favorites";
            case "score" -> "rating";
            default -> requested;
        };
    }

    private void replaceProvidedRelations(Long animeId, AnimeSaveRequest request) {
        if (request.genreIds() != null) {
            genreRelationMapper.deleteByAnimeId(animeId);
            replaceGenres(animeId, request.genreIds());
        }
        if (request.staff() != null) {
            staffRelationMapper.deleteByAnimeId(animeId);
            replaceStaff(animeId, request.staff());
        }
        if (request.platforms() != null) {
            platformRelationMapper.deleteByAnimeId(animeId);
            replacePlatforms(animeId, request.platforms());
        }
    }

    private void replaceGenres(Long animeId, List<Long> genreIds) {
        Set<Long> uniqueIds = new HashSet<>(genreIds);
        if (uniqueIds.size() != genreIds.size()) {
            throw BusinessException.badRequest("类型列表不能包含重复项");
        }
        if (!genreIds.isEmpty() && genreMapper.selectBatchIds(uniqueIds).size() != uniqueIds.size()) {
            throw BusinessException.badRequest("包含不存在的动漫类型");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long genreId : genreIds) {
            genreRelationMapper.insertRelation(AnimeGenreRelation.builder()
                    .animeId(animeId)
                    .genreId(genreId)
                    .createdAt(now)
                    .build());
        }
    }

    private void replaceStaff(Long animeId, List<AnimeSaveRequest.StaffCreditRequest> credits) {
        Set<String> uniqueCredits = new HashSet<>();
        for (AnimeSaveRequest.StaffCreditRequest credit : credits) {
            String roleType = normalizeStaffRole(credit.roleType(), credit.roleName());
            if (!uniqueCredits.add(credit.staffId() + ":" + roleType)) {
                throw BusinessException.badRequest("制作人员职务列表不能包含重复项");
            }
            if (staffMapper.selectById(credit.staffId()) == null) {
                throw BusinessException.badRequest("包含不存在的制作人员");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (AnimeSaveRequest.StaffCreditRequest credit : credits) {
            staffRelationMapper.insertRelation(AnimeStaffRelation.builder()
                    .animeId(animeId)
                    .staffId(credit.staffId())
                    .roleType(normalizeStaffRole(credit.roleType(), credit.roleName()))
                    .createdAt(now)
                    .build());
        }
    }

    private void replacePlatforms(Long animeId, List<AnimeSaveRequest.PlatformLinkRequest> links) {
        Set<Long> uniquePlatforms = new HashSet<>();
        for (AnimeSaveRequest.PlatformLinkRequest link : links) {
            if (!uniquePlatforms.add(link.platformId())) {
                throw BusinessException.badRequest("播放平台列表不能包含重复项");
            }
            if (platformMapper.selectById(link.platformId()) == null) {
                throw BusinessException.badRequest("包含不存在的播放平台");
            }
            if (link.availableFrom() != null && link.availableUntil() != null
                    && link.availableUntil().isBefore(link.availableFrom())) {
                throw BusinessException.badRequest("平台下架日期不能早于上架日期");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (AnimeSaveRequest.PlatformLinkRequest link : links) {
            platformRelationMapper.insertRelation(AnimePlatformRelation.builder()
                    .animeId(animeId)
                    .platformId(link.platformId())
                    .externalUrl(firstNonBlank(link.externalUrl(), link.watchUrl()))
                    .availableFrom(link.availableFrom())
                    .availableUntil(link.availableUntil())
                    .createdAt(now)
                    .build());
        }
    }

    private void clearRelations(Long animeId) {
        genreRelationMapper.deleteByAnimeId(animeId);
        staffRelationMapper.deleteByAnimeId(animeId);
        platformRelationMapper.deleteByAnimeId(animeId);
    }

    private String normalizeStaffRole(String roleType, String roleName) {
        String raw = firstNonBlank(roleType, roleName);
        if (raw == null) {
            throw BusinessException.badRequest("制作人员职务不能为空");
        }
        String normalized = switch (raw.trim()) {
            case "导演" -> "DIRECTOR";
            case "系列构成", "编剧" -> "SERIES_COMPOSITION";
            case "音乐" -> "MUSIC";
            case "原作者", "原作" -> "ORIGINAL_CREATOR";
            case "角色设计" -> "CHARACTER_DESIGN";
            case "声优" -> "VOICE_ACTOR";
            case "制片人" -> "PRODUCER";
            case "作画监督" -> "ANIMATION_DIRECTOR";
            default -> raw.trim().toUpperCase(Locale.ROOT);
        };
        if (!ALLOWED_STAFF_ROLES.contains(normalized)) {
            throw BusinessException.badRequest("制作人员职务不在数据库 CHECK 约束允许范围内");
        }
        return normalized;
    }

    private LocalDate resolveReleaseDate(LocalDate releaseDate, Integer releaseYear) {
        if (releaseDate != null) {
            return releaseDate;
        }
        return releaseYear == null ? null : LocalDate.of(releaseYear, 1, 1);
    }

    private void validateDates(LocalDate releaseDate, LocalDate endDate) {
        if (releaseDate != null && endDate != null && endDate.isBefore(releaseDate)) {
            throw BusinessException.badRequest("结束日期不能早于首播日期");
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
