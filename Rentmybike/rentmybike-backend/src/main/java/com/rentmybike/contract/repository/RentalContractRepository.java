package com.rentmybike.contract.repository;

import com.rentmybike.contract.entity.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, UUID> {

    @Query("""
            SELECT c FROM RentalContract c
            WHERE c.deletedAt IS NULL AND c.booking.id = :bookingId
            """)
    Optional<RentalContract> findByBookingId(@Param("bookingId") UUID bookingId);
}
