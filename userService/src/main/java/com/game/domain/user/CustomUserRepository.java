package com.game.domain.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomUserRepository extends JpaRepository<CustomUser, Long> {

	Optional<CustomUser> findByUid(String uid);

}
