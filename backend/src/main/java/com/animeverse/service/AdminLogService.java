package com.animeverse.service;

import com.animeverse.common.PageResult;
import com.animeverse.mapper.OperationLogMapper;
import com.animeverse.vo.OperationLogVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLogVO> page(
            long page,
            long size,
            String keyword,
            String targetType
    ) {
        IPage<OperationLogVO> result = operationLogMapper.selectLogPage(
                Page.of(page, size),
                keyword == null || keyword.isBlank() ? null : keyword.trim(),
                targetType == null || targetType.isBlank() ? null : targetType.trim()
        );
        return PageResult.of(result);
    }
}
