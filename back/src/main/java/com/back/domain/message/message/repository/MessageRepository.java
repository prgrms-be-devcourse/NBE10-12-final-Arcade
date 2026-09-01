package com.back.domain.message.message.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.message.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "recipient"})
    Page<Message> findByRecipient(Member recipient, Pageable page);

    @EntityGraph(attributePaths = {"sender", "recipient"})
    Page<Message> findBySender(Member sender, Pageable page);
}
