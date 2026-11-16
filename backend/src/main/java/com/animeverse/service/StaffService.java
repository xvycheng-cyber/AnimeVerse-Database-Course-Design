package com.animeverse.service;

import com.animeverse.common.BusinessException;
import com.animeverse.common.PageResult;
import com.animeverse.dto.StaffSaveRequest;
import com.animeverse.entity.AnimeStaff;
import com.animeverse.mapper.AnimeStaffMapper;
import com.animeverse.mapper.AnimeStaffRelationMapper;
import com.animeverse.vo.StaffVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final AnimeStaffMapper staffMapper;
    private final AnimeStaffRelationMapper relationMapper;

    public PageResult<StaffVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<AnimeStaff> wrapper = new LambdaQueryWrapper<AnimeStaff>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(AnimeStaff::getStaffName, keyword)
                        .or()
                        .like(AnimeStaff::getStaffNameJp, keyword))
                .orderByAsc(AnimeStaff::getStaffName);
        Page<AnimeStaff> result = staffMapper.selectPage(Page.of(page, size), wrapper);
        List<StaffVO> records = result.getRecords().stream().map(StaffVO::from).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public StaffVO create(StaffSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        AnimeStaff staff = AnimeStaff.builder()
                .staffName(resolveName(request))
                .staffNameJp(resolveOriginalName(request))
                .country(resolveCountry(request))
                .birthDate(request.birthDate())
                .bio(resolveBio(request))
                .avatarUrl(trimToNull(request.avatarUrl()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        staffMapper.insert(staff);
        return StaffVO.from(staff);
    }

    @Transactional
    public StaffVO update(Long staffId, StaffSaveRequest request) {
        AnimeStaff staff = requireStaff(staffId);
        staff.setStaffName(resolveName(request));
        staff.setStaffNameJp(resolveOriginalName(request));
        if (resolveCountry(request) != null) {
            staff.setCountry(resolveCountry(request));
        }
        staff.setBirthDate(request.birthDate());
        staff.setBio(resolveBio(request));
        staff.setAvatarUrl(trimToNull(request.avatarUrl()));
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        return StaffVO.from(requireStaff(staffId));
    }

    @Transactional
    public void delete(Long staffId) {
        requireStaff(staffId);
        if (relationMapper.countByStaffId(staffId) > 0) {
            throw BusinessException.conflict("制作人员仍有关联作品，不能删除");
        }
        staffMapper.deleteById(staffId);
    }

    public AnimeStaff requireStaff(Long staffId) {
        AnimeStaff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw BusinessException.notFound("制作人员不存在");
        }
        return staff;
    }

    private String resolveName(StaffSaveRequest request) {
        String name = firstNonBlank(request.staffName(), request.name());
        if (name == null) {
            throw BusinessException.badRequest("制作人员姓名不能为空");
        }
        return name;
    }

    private String resolveOriginalName(StaffSaveRequest request) {
        return firstNonBlank(request.staffNameJp(), request.originalName());
    }

    private String resolveCountry(StaffSaveRequest request) {
        String country = firstNonBlank(request.country(), request.nationality());
        return country == null ? null : country;
    }

    private String resolveBio(StaffSaveRequest request) {
        return firstNonBlank(request.bio(), request.biography());
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
