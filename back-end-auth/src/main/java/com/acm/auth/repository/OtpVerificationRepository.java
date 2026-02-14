package com.acm.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acm.auth.entity.OtpVerification;
import com.acm.auth.enums.OtpPurpose;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);
}
