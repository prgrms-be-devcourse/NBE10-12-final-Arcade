package com.back.domain.message.message.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.message.message.dtos.MessageDetailDto;
import com.back.domain.message.message.dtos.MessageDto;
import com.back.domain.message.message.dtos.MessageListDto;
import com.back.domain.message.message.dtos.MessageMemberDto;
import com.back.domain.message.message.entity.Message;
import com.back.domain.message.message.repository.MessageRepository;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.notification.notification.service.NotificationService;
import com.back.global.dto.PageDto;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final NotificationService notificationService;

    public enum MessageFilterOption { RECEIVED, SENT }

    @Transactional
    public MessageDto send(Member sender, long recipientId, String content) {
        if (sender.getId().equals(recipientId)) {
            throw new ServiceException("409-1", "본인에게는 쪽지를 보낼 수 없습니다.");
        }

        Member recipient = memberRepository.findById(recipientId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 회원입니다."));

        Message message = messageRepository.save(new Message(sender, recipient, content));
        notificationService.create(
                recipient,
                NotificationType.MESSAGE_RECEIVED,
                sender.getName() + "님이 보낸 새 쪽지가 도착했습니다."
        );

        return new MessageDto(message);
    }

    public PageDto<MessageListDto> getList(Member actor, MessageFilterOption option, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createDate"),
                Sort.Order.desc("id")
        ));

        Page<Message> messages = option == MessageFilterOption.RECEIVED
                ? messageRepository.findByRecipient(actor, pageable)
                : messageRepository.findBySender(actor, pageable);

        Map<Long, MessageMemberDto> membersById
                = memberDtos(
                        messages.getContent()
                                .stream()
                                .flatMap(message ->
                                        Stream.of(
                                                message.getSender(),
                                                message.getRecipient()
                                        )
                                )
                                .toList()
        );

        return new PageDto<>(
                messages.map(
                        message -> toListDto(message, membersById))
        );
    }

    @Transactional
    public List<MessageDto> read(Member actor, List<Long> ids) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new ServiceException("400-1", "ids에 중복된 쪽지가 포함되어 있습니다.");
        }

        List<Message> messages = messageRepository.findAllById(uniqueIds);
        if (messages.size() != uniqueIds.size()) {
            throw new ServiceException("404-1", "존재하지 않는 쪽지입니다.");
        }

        Map<Long, Message> messagesById = new HashMap<>();
        for (Message message : messages) {
            if (!message.getRecipient().getId().equals(actor.getId())) {
                throw new ServiceException("403-1", "본인과 관련된 쪽지만 조회/처리할 수 있습니다.");
            }
            messagesById.put(message.getId(), message);
        }

        return ids.stream()
                .map(messagesById::get)
                .peek(message -> markAsRead(message, actor))
                .map(MessageDto::new)
                .toList();
    }

    @Transactional
    public MessageDetailDto getMessage(Member actor, long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 쪽지입니다."));
        boolean isSender = message.getSender().getId().equals(actor.getId());
        boolean isRecipient = message.getRecipient().getId().equals(actor.getId());
        if (!isSender && !isRecipient) {
            throw new ServiceException("403-1", "본인과 관련된 쪽지만 조회/처리할 수 있습니다.");
        }
        if (isRecipient) {
            markAsRead(message, actor);
        }
        return toDetailDto(message);
    }

    private void markAsRead(Message message, Member reader) {
        if (message.isRead()) {
            return;
        }

        message.read();
        notificationService.create(
                message.getSender(),
                NotificationType.MESSAGE_READ,
                reader.getName() + "님이 보낸 쪽지를 읽었습니다."
        );
    }

    private MessageListDto toListDto(Message message, Map<Long, MessageMemberDto> membersById) {
        return new MessageListDto(message,
                membersById.get(message.getSender().getId()),
                membersById.get(message.getRecipient().getId())
        );
    }

    private MessageDetailDto toDetailDto(Message message) {
        Map<Long, MessageMemberDto> membersById = memberDtos(List.of(message.getSender(), message.getRecipient()));
        return new MessageDetailDto(
                message,
                membersById.get(message.getSender().getId()),
                membersById.get(message.getRecipient().getId())
        );
    }

    private Map<Long, MessageMemberDto> memberDtos(Collection<Member> members) {
        if (members.isEmpty()) {
            return Map.of();
        }

        Map<Long, Member> membersById = members.stream().collect(Collectors.toMap(
                Member::getId,
                member -> member,
                (first, ignored) -> first,
                LinkedHashMap::new
        ));

        // Collectors.toMap does not permit null values. A profile can legitimately
        // have no nickname until its owner completes profile setup, so retain that
        // null value in a map implementation that supports it.
        Map<Long, String> nicknameByMemberId = new HashMap<>();
        memberProfileRepository.findByMember_IdIn(membersById.keySet())
                .forEach(profile -> nicknameByMemberId.put(
                        profile.getMember().getId(),
                        profile.getNickname()
                ));

        return membersById.values().stream().collect(Collectors.toMap(
                Member::getId,
                member -> new MessageMemberDto(member.getId(), member.getName(), nicknameByMemberId.get(member.getId())),
                (first, ignored) -> first,
                LinkedHashMap::new
        ));
    }
}
