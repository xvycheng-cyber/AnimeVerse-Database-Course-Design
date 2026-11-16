package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.service.GenreService;
import com.animeverse.vo.GenreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ApiResponse<List<GenreVO>> list() {
        return ApiResponse.success(genreService.list());
    }
}
