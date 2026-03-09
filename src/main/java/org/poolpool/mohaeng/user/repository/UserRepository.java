package org.poolpool.mohaeng.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.poolpool.mohaeng.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	//이메일 중복 조회
	boolean existsByEmail(String email);

	//로그인 시 이메일로 회원 정보 조회
	Optional<UserEntity> findByEmail(String email);

	//이메일 찾기
	List<UserEntity> findAllByNameAndPhone(String name, String phone);

	//비밀번호 찾기
	UserEntity findByEmailAndPhone(String email, String phone);

	//랜덤 비밀번호 업데이트
	@Modifying
    @Query("""
        update UserEntity u
        set u.userPwd = :renewPwd,
            u.updatedAt = :updatedAt
        where u.userId = :userId 
        and u.email = :email
    """)
    int updateRenewPwd(@Param("userId") Long userId, @Param("email") String email, 
    		@Param("renewPwd") String renewPwd, @Param("updatedAt") LocalDateTime updatedAt);
}
