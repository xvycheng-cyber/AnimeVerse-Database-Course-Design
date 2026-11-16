package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.SlugUtils;
import com.animeverse.dto.GenreSaveRequest;
import com.animeverse.entity.AnimeGenre;
import com.animeverse.mapper.AnimeGenreMapper;
import com.animeverse.mapper.AnimeGenreRelationMapper;
import com.animeverse.vo.GenreVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final AnimeGenreMapper genreMapper;
    private final AnimeGenreRelationMapper relationMapper;

    public List<GenreVO> list() {
        return genreMapper.selectList(
                        new LambdaQueryWrapper<AnimeGenre>().orderByAsc(AnimeGenre::getId)
                ).stream()
                .map(GenreVO::from)
                .toList();
    }

    @Transactional
    public GenreVO create(GenreSaveRequest request) {
        String genreName = request.name().trim();
        String slug = SlugUtils.normalizeExternalSlug(request.slug(), genreName, "genre");
        ensureUnique(genreName, slug, null);
        AnimeGenre genre = AnimeGenre.builder()
                .genreName(genreName)
                .genreSlug(slug)
                .description(trimToNull(request.description()))
                .createdAt(LocalDateTime.now())
                .build();
        genreMapper.insert(genre);
        return GenreVO.from(genre);
    }

    @Transactional
    public GenreVO update(Long genreId, GenreSaveRequest request) {
        AnimeGenre genre = requireGenre(genreId);
        String genreName = request.name().trim();
        String slug = SlugUtils.normalizeExternalSlug(request.slug(), genreName, "genre");
        ensureUnique(genreName, slug, genreId);
        genre.setGenreName(genreName);
        genre.setGenreSlug(slug);
        genre.setDescription(trimToNull(request.description()));
        genreMapper.updateById(genre);
        return GenreVO.from(genre);
    }

    @Transactional
    public void delete(Long genreId) {
        requireGenre(genreId);
        if (relationMapper.countByGenreId(genreId) > 0) {
            throw BusinessException.conflict("类型仍被动漫引用，不能删除");
        }
        genreMapper.deleteById(genreId);
    }

    public AnimeGenre requireGenre(Long genreId) {
        AnimeGenre genre = genreMapper.selectById(genreId);
        if (genre == null) {
            throw BusinessException.notFound("动漫类型不存在");
        }
        return genre;
    }

    private void ensureUnique(String genreName, String slug, Long excludeId) {
        AnimeGenre sameName = genreMapper.selectOne(
                new LambdaQueryWrapper<AnimeGenre>()
                        .eq(AnimeGenre::getGenreName, genreName)
                        .last("LIMIT 1")
        );
        if (sameName != null && !sameName.getId().equals(excludeId)) {
            throw BusinessException.conflict("动漫类型名称已存在");
        }
        AnimeGenre sameSlug = genreMapper.selectOne(
                new LambdaQueryWrapper<AnimeGenre>()
                        .eq(AnimeGenre::getGenreSlug, slug)
                        .last("LIMIT 1")
        );
        if (sameSlug != null && !sameSlug.getId().equals(excludeId)) {
            throw BusinessException.conflict("动漫类型 slug 已存在");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
