package com.memorydrawer.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.memorydrawer.auth.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	boolean existsByEmail(String email);

	Optional<UserAccount> findByEmail(String email);
}
