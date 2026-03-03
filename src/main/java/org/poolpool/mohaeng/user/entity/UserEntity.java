package org.poolpool.mohaeng.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserRole;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.poolpool.mohaeng.user.type.UserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "USER_PWD", length = 255)
    private String userPwd;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "PROFILE_IMG", length = 255)
    private String profileImg;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_TYPE", nullable = false, length = 20)
    private UserType userType;

    @Column(name = "BUSINESS_NUM", length = 20)
    private String businessNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "SIGNUP_TYPE", nullable = false, length = 20)
    private SignupType signupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", nullable = false, length = 20)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_STATUS", nullable = false, length = 20)
    private UserStatus userStatus;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "LAST_LOGIN_AT", nullable = false)
    private LocalDate lastLoginAt;

    @Column(name = "WITHREASON_ID")
    private Integer withReasonId;

    @Column(name = "WITHDRAWAL_REASON", columnDefinition = "TEXT")
    private String withdrawalReason;
    
    @PrePersist
    public void prePersist() {
    	userRole = UserRole.USER;
    	userStatus = UserStatus.ACTIVE;
    	createdAt = LocalDateTime.now();
    	lastLoginAt = LocalDate.now();
    }
    
    @PreUpdate
    public void preUpdate() {
    	updatedAt = LocalDateTime.now();
    }
}
