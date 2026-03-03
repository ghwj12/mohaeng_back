package org.poolpool.mohaeng.auth.token.refresh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token")
public class RefreshTokenEntity {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "USER_ID", length = 50, nullable = false)
    private Long userId;

    @Column(name = "TOKEN_VALUE", length = 512, nullable = false, unique = true)
    private String tokenValue;

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;
}
