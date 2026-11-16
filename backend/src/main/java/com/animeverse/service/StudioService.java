package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.StudioSaveRequest;
import com.animeverse.entity.Anime;
import com.animeverse.entity.AnimeStudio;
import com.animeverse.mapper.AnimeMapper;
import com.animeverse.mapper.AnimeStudioMapper;
import com.animeverse.vo.StudioVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudioService {

    private final AnimeStudioMapper studioMapper;
    private final AnimeMapper animeMapper;

    public PageResult<StudioVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<AnimeStudio> wrapper = new LambdaQueryWrapper<AnimeStudio>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(AnimeStudio::getStudioName, keyword)
                        .or()
                        .like(AnimeStudio::getStudioNameEn, keyword))
                .orderByAsc(AnimeStudio::getStudioName);
        Page<AnimeStudio> result = studioMapper.selectPage(Page.of(page, size), wrapper);
        List<StudioVO> records = result.getRecords().stream().map(StudioVO::from).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public StudioVO get(Long studioId) {
        return StudioVO.from(requireStudio(studioId));
    }

    @Transactional
    public StudioVO create(StudioSaveRequest request) {
        String studioName = resolveName(request);
        ensureUniqueName(studioName, null);
        LocalDateTime now = LocalDateTime.now();
        AnimeStudio studio = AnimeStudio.builder()
                .studioName(studioName)
                .studioNameEn(resolveOriginalName(request))
                .country(request.country() == null || request.country().isBlank() ? "日本" : request.country().trim())
                .foundedYear(request.foundedYear())
                .description(trimToNull(request.description()))
                .logoUrl(trimToNull(request.logoUrl()))
                .websiteUrl(resolveWebsite(request))
                .createdAt(now)
                .updatedAt(now)
                .build();
        studioMapper.insert(studio);
        return StudioVO.from(studio);
    }

    @Transactional
    public StudioVO update(Long studioId, StudioSaveRequest request) {
        AnimeStudio studio = requireStudio(studioId);
        String studioName = resolveName(request);
        ensureUniqueName(studioName, studioId);
        studio.setStudioName(studioName);
        studio.setStudioNameEn(resolveOriginalName(request));
        if (request.country() != null && !request.country().isBlank()) {
            studio.setCountry(request.country().trim());
        }
        studio.setFoundedYear(request.foundedYear());
        studio.setDescription(trimToNull(request.description()));
        studio.setLogoUrl(trimToNull(request.logoUrl()));
        studio.setWebsiteUrl(resolveWebsite(request));
        studio.setUpdatedAt(LocalDateTime.now());
        studioMapper.updateById(studio);
        return get(studioId);
    }

    @Transactional
    public void delete(Long studioId) {
        requireStudio(studioId);
        long animeCount = animeMapper.selectCount(
                new LambdaQueryWrapper<Anime>().eq(Anime::getStudioId, studioId)
        );
        if (animeCount > 0) {
            throw BusinessException.conflict("厂商仍有动漫作品，不能删除");
        }
        studioMapper.deleteById(studioId);
    }

    public AnimeStudio requireStudio(Long studioId) {
        AnimeStudio studio = studioMapper.selectById(studioId);
        if (studio == null) {
            throw BusinessException.notFound("制作厂商不存在");
        }
        return studio;
    }

    private void ensureUniqueName(String name, Long excludeId) {
        AnimeStudio existing = studioMapper.selectOne(
                new LambdaQueryWrapper<AnimeStudio>().eq(AnimeStudio::getStudioName, name).last("LIMIT 1")
        );
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw BusinessException.conflict("制作厂商名称已存在");
        }
    }

    private String resolveName(StudioSaveRequest request) {
        String name = firstNonBlank(request.studioName(), request.name());
        if (name == null) {
            throw BusinessException.badRequest("制作厂商名称不能为空");
        }
        return name;
    }

    private String resolveOriginalName(StudioSaveRequest request) {
        return firstNonBlank(request.studioNameEn(), request.originalName());
    }

    private String resolveWebsite(StudioSaveRequest request) {
        return firstNonBlank(request.websiteUrl(), request.website());
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
