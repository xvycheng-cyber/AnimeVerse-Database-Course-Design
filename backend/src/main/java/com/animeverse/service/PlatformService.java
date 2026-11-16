package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.SlugUtils;
import com.animeverse.dto.PlatformSaveRequest;
import com.animeverse.entity.StreamingPlatform;
import com.animeverse.mapper.AnimePlatformRelationMapper;
import com.animeverse.mapper.StreamingPlatformMapper;
import com.animeverse.vo.PlatformVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private final StreamingPlatformMapper platformMapper;
    private final AnimePlatformRelationMapper relationMapper;

    public List<PlatformVO> list() {
        return platformMapper.selectList(
                        new LambdaQueryWrapper<StreamingPlatform>()
                                .orderByAsc(StreamingPlatform::getPlatformName)
                ).stream()
                .map(PlatformVO::from)
                .toList();
    }

    @Transactional
    public PlatformVO create(PlatformSaveRequest request) {
        String platformName = request.name().trim();
        String code = SlugUtils.normalizeExternalSlug(request.code(), platformName, "platform");
        String websiteUrl = requireWebsite(request);
        ensureUnique(platformName, code, null);
        LocalDateTime now = LocalDateTime.now();
        StreamingPlatform platform = StreamingPlatform.builder()
                .platformName(platformName)
                .platformCode(code)
                .websiteUrl(websiteUrl)
                .country(trimToNull(request.country()))
                .isActive(request.active() == null || request.active() ? 1 : 0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        platformMapper.insert(platform);
        return PlatformVO.from(platform);
    }

    @Transactional
    public PlatformVO update(Long platformId, PlatformSaveRequest request) {
        StreamingPlatform platform = requirePlatform(platformId);
        String platformName = request.name().trim();
        String code = SlugUtils.normalizeExternalSlug(request.code(), platformName, "platform");
        ensureUnique(platformName, code, platformId);
        platform.setPlatformName(platformName);
        platform.setPlatformCode(code);
        platform.setWebsiteUrl(requireWebsite(request));
        if (request.country() != null && !request.country().isBlank()) {
            platform.setCountry(request.country().trim());
        }
        if (request.active() != null) {
            platform.setIsActive(request.active() ? 1 : 0);
        }
        platform.setUpdatedAt(LocalDateTime.now());
        platformMapper.updateById(platform);
        return PlatformVO.from(requirePlatform(platformId));
    }

    @Transactional
    public void delete(Long platformId) {
        requirePlatform(platformId);
        if (relationMapper.countByPlatformId(platformId) > 0) {
            throw BusinessException.conflict("播放平台仍关联动漫，不能删除");
        }
        platformMapper.deleteById(platformId);
    }

    public StreamingPlatform requirePlatform(Long platformId) {
        StreamingPlatform platform = platformMapper.selectById(platformId);
        if (platform == null) {
            throw BusinessException.notFound("播放平台不存在");
        }
        return platform;
    }

    private void ensureUnique(String name, String code, Long excludeId) {
        StreamingPlatform sameName = platformMapper.selectOne(
                new LambdaQueryWrapper<StreamingPlatform>()
                        .eq(StreamingPlatform::getPlatformName, name)
                        .last("LIMIT 1")
        );
        if (sameName != null && !sameName.getId().equals(excludeId)) {
            throw BusinessException.conflict("播放平台名称已存在");
        }
        StreamingPlatform sameCode = platformMapper.selectOne(
                new LambdaQueryWrapper<StreamingPlatform>()
                        .eq(StreamingPlatform::getPlatformCode, code)
                        .last("LIMIT 1")
        );
        if (sameCode != null && !sameCode.getId().equals(excludeId)) {
            throw BusinessException.conflict("播放平台编码已存在");
        }
    }

    private String requireWebsite(PlatformSaveRequest request) {
        if (request.websiteUrl() == null || request.websiteUrl().isBlank()) {
            throw BusinessException.badRequest("播放平台官网地址不能为空");
        }
        return request.websiteUrl().trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
