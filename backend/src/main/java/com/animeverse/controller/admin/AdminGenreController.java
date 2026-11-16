package com.animeverse.controller.admin;

import com.animeverse.common.ApiResponse;
import com.animeverse.common.OperationLog;
import com.animeverse.dto.GenreSaveRequest;
import com.animeverse.service.GenreService;
import com.animeverse.vo.GenreVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/genres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenreController {

    private final GenreService genreService;

    @GetMapping
    public ApiResponse<List<GenreVO>> list() {
        return ApiResponse.success(genreService.list());
    }

    @PostMapping
    @OperationLog(module = "GENRE", action = "CREATE")
    public ResponseEntity<ApiResponse<GenreVO>> create(@Valid @RequestBody GenreSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(genreService.create(request)));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "GENRE", action = "UPDATE")
    public ApiResponse<GenreVO> update(
            @PathVariable Long id,
            @Valid @RequestBody GenreSaveRequest request
    ) {
        return ApiResponse.success(genreService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "GENRE", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        genreService.delete(id);
        return ApiResponse.success();
    }
}
