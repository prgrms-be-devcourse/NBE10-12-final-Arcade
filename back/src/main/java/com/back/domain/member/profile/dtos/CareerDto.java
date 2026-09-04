package com.back.domain.member.profile.dtos;

import com.back.domain.member.profile.entity.MemberProfileCareer;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CareerDto(
        long id,
        LocalDate startDate,
        /** null 이면 재직중 */
        LocalDate endDate,
        String role,
        String org,
        String description
) {
    public CareerDto(MemberProfileCareer career) {
        this(
                career.getId(),
                career.getStartDate(),
                career.getEndDate(),
                career.getRole(),
                career.getOrg(),
                career.getDescription()
        );
    }
}
