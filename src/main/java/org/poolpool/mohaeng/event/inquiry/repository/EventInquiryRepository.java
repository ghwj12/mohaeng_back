package org.poolpool.mohaeng.event.inquiry.repository;

import java.util.List;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto;
import org.poolpool.mohaeng.event.inquiry.entity.EventInquiryEntity;
import org.poolpool.mohaeng.user.entity.UserEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventInquiryRepository extends JpaRepository<EventInquiryEntity, Long> {

    // ✅ 행사상세 문의 목록(DTO)
    @Query("""
        select new org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto(
            i.inqId,
            i.eventId,
            i.userId,
            u.name,
            i.content,
            i.replyContent,
            i.replyId,
            i.replyDate,
            i.status,
            i.createdAt,
            e.title,
            e.thumbnail
        )
        from EventInquiryEntity i
        join UserEntity u on u.userId = i.userId
        join EventEntity e on e.eventId = i.eventId
        where i.eventId = :eventId
        order by i.createdAt desc
    """)
    List<EventInquiryDto> findInquiryListDtoByEventId(@Param("eventId") Long eventId);

    // ✅ WRITTEN: 내가 작성한 문의 (삭제된 행사 제외)
    @Query(
        value = """
            select new org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto(
                i.inqId,
                i.eventId,
                i.userId,
                u.name,
                i.content,
                i.replyContent,
                i.replyId,
                i.replyDate,
                i.status,
                i.createdAt,
                e.title,
                e.thumbnail
            )
            from EventInquiryEntity i
            join UserEntity u on u.userId = i.userId
            join EventEntity e on e.eventId = i.eventId
            where i.userId = :me
            and e.eventStatus <> 'DELETED'
            order by i.createdAt desc
        """,
        countQuery = """
            select count(i)
            from EventInquiryEntity i
            join EventEntity e on e.eventId = i.eventId
            where i.userId = :me
            and e.eventStatus <> 'DELETED'
        """
    )
    Page<EventInquiryDto> findWrittenForMypageDto(@Param("me") Long me, Pageable pageable);

    // ✅ RECEIVED: 내가 주최한 행사에 달린 문의 (삭제된 행사 제외)
    @Query(
        value = """
            select new org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto(
                i.inqId,
                i.eventId,
                i.userId,
                u.name,
                i.content,
                i.replyContent,
                i.replyId,
                i.replyDate,
                i.status,
                i.createdAt,
                e.title,
                e.thumbnail
            )
            from EventInquiryEntity i
            join UserEntity u on u.userId = i.userId
            join EventEntity e on e.eventId = i.eventId
            where e.host.userId = :me
            and e.eventStatus <> 'DELETED'
            order by i.createdAt desc
        """,
        countQuery = """
            select count(i)
            from EventInquiryEntity i
            join EventEntity e on e.eventId = i.eventId
            where e.host.userId = :me
            and e.eventStatus <> 'DELETED'
        """
    )
    Page<EventInquiryDto> findReceivedForMypageDto(@Param("me") Long me, Pageable pageable);

    // ✅ ALL: 내가 작성 OR 내가 주최 행사에 달린 문의 (삭제된 행사 제외)
    @Query(
        value = """
            select new org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto(
                i.inqId,
                i.eventId,
                i.userId,
                u.name,
                i.content,
                i.replyContent,
                i.replyId,
                i.replyDate,
                i.status,
                i.createdAt,
                e.title,
                e.thumbnail
            )
            from EventInquiryEntity i
            join UserEntity u on u.userId = i.userId
            join EventEntity e on e.eventId = i.eventId
            where ((i.userId = :me) or (e.host.userId = :me))
            and e.eventStatus <> 'DELETED'
            order by i.createdAt desc
        """,
        countQuery = """
            select count(i)
            from EventInquiryEntity i
            join EventEntity e on e.eventId = i.eventId
            where ((i.userId = :me) or (e.host.userId = :me))
            and e.eventStatus <> 'DELETED'
        """
    )
    Page<EventInquiryDto> findAllForMypageDto(@Param("me") Long me, Pageable pageable);
}
