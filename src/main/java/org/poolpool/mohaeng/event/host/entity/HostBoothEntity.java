package org.poolpool.mohaeng.event.host.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "host_booth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HostBoothEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOOTH_ID")
    private Long boothId;

    @Column(name = "EVENT_ID", nullable = false)
    private Long eventId;

    @Column(name = "BOOTH_NAME", length = 100, nullable = false)
    private String boothName;

    @Column(name = "BOOTH_PRICE", nullable = false)
    @Builder.Default
    private Integer boothPrice = 0;

    @Column(name = "BOOTH_SIZE", length = 50)
    private String boothSize;

    @Column(name = "BOOTH_NOTE", length = 255)
    private String boothNote;

    @Column(name = "TOTAL_COUNT", nullable = false)
    @Builder.Default
    private Integer totalCount = 1;

    @Column(name = "REMAIN_COUNT", nullable = false)
    @Builder.Default
    private Integer remainCount = 1;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}