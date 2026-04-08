// ─────────────────────────────────────────────────────────────────────────────
// FILE: repository/DriverRepository.java
// ─────────────────────────────────────────────────────────────────────────────
package com.f1report.repository;

import com.f1report.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DriverRepository – Spring Data JPA repository for Driver entities.
 *
 * By extending JpaRepository<Driver, Long>, Spring auto-generates:
 *   • save(driver)           → INSERT or UPDATE
 *   • findById(id)           → SELECT by PK
 *   • findAll()              → SELECT *
 *   • deleteById(id)         → DELETE
 *   • count()                → SELECT COUNT(*)
 * ... and more, all without writing a single line of SQL.
 *
 * Real-world analogy: it's like inheriting a full filing cabinet system.
 * You just say what type of documents go in it (Driver) and what the key
 * looks like (Long = the numeric ID), and the system provides all the
 * standard operations for free.
 *
 * Custom methods below follow Spring's method-name query convention:
 * findBy<FieldName> → generates "WHERE field_name = ?" automatically.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    /**
     * Find a driver by their Ergast driverId string (e.g., "max_verstappen").
     * Used to avoid inserting duplicate drivers when syncing from the API.
     *
     * Generated SQL: SELECT * FROM drivers WHERE driver_id = ?
     */
    Optional<Driver> findByDriverId(String driverId);

    /**
     * Find a driver by their three-letter broadcast code.
     * Generated SQL: SELECT * FROM drivers WHERE code = ?
     */
    Optional<Driver> findByCode(String code);

    /**
     * Check if a driver with this Ergast ID already exists in our DB.
     * Generated SQL: SELECT COUNT(*) > 0 FROM drivers WHERE driver_id = ?
     */
    boolean existsByDriverId(String driverId);
}
