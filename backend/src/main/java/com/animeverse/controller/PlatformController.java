package com.animeverse.controller;

import com.animeverse.common.ApiResponse;
import com.animeverse.service.PlatformService;
import com.animeverse.vo.PlatformVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping
    public ApiResponse<List<PlatformVO>> list() {
        return ApiResponse.success(platformService.list());
    }
}
