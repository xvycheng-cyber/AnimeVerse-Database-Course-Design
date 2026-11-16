package com.animeverse.vo;

import com.animeverse.entity.AnimeGenre;

import java.time.LocalDateTime;

public record GenreVO(
        Long id,
        String name,
        String slug,
        String description,
        String genreName,
        String genreSlug,
        LocalDateTime createdAt
) {
    public static GenreVO from(AnimeGenre genre) {
        return new GenreVO(
                genre.getId(),
                genre.getGenreName(),
                genre.getGenreSlug(),
                genre.getDescription(),
                genre.getGenreName(),
                genre.getGenreSlug(),
                genre.getCreatedAt()
        );
    }
}
