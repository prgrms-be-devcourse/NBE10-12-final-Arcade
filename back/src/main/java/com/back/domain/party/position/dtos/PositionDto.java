package com.back.domain.party.position.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.position.entity.Position;

public record PositionDto(
    long id,
    PositionType type,
    int capacity,
    int filledCount
) {
    public PositionDto(Position position) {
        this(
            position.getId(),
            position.getType(),
            position.getCapacity(),
            position.getFilledCount()
        );
    }
}
