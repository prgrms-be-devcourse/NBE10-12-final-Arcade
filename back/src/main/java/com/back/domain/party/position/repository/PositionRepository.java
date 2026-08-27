package com.back.domain.party.position.repository;

import com.back.domain.party.position.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    // 승인 처리 시 @Version 값을 최신으로 다시 읽기 위한 명시적 낙관적 락 조회
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Position p where p.id = :id")
    Optional<Position> findByIdForUpdate(@Param("id") int id);
}
