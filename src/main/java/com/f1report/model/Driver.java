package com.f1report.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Driver – JPA entity that maps to the "drivers" table in PostgreSQL.
 *
 * JPA (Java Persistence API) works like a smart translator:
 * when you call driverRepo.save(driver), Hibernate converts the Driver object
 * into an INSERT/UPDATE SQL statement. When you call driverRepo.findById(id),
 * it runs a SELECT and maps the row back to a Driver object.
 *
 * Real-world analogy: think of a Driver object as a card in a Rolodex.
 * Hibernate is the librarian who files/retrieves those cards from the PostgreSQL
 * filing cabinet and translates between "human-readable card" and "database row".
 *
 * @Entity   → Hibernate manages this class as a database table
 * @Table    → specifies the exact table name (avoids naming ambiguity)
 * @Id      → this field is the primary key
 * @Column  → controls column name, nullability, length constraints
 *
 * Lombok annotations eliminate boilerplate:
 * @Data            = @Getter + @Setter + @ToString + @EqualsAndHashCode
 * @Builder         = fluent builder: Driver.builder().driverId("verstappen").build()
 * @NoArgsConstructor  = no-arg constructor (required by JPA spec)
 * @AllArgsConstructor = all-fields constructor (used by @Builder internally)
 */
@Entity
@Table(name = "drivers",
       uniqueConstraints = @UniqueConstraint(columnNames = "driver_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    /**
     * Auto-incrementing surrogate primary key.
     * GenerationType.IDENTITY lets PostgreSQL's SERIAL/sequence handle this.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ergast's string identifier for the driver (e.g., "max_verstappen").
     * Used as a business key when de-duplicating drivers from the API.
     */
    @Column(name = "driver_id", nullable = false, unique = true, length = 100)
    private String driverId;

    @Column(name = "given_name", nullable = false, length = 100)
    private String givenName;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "date_of_birth", length = 20)
    private String dateOfBirth;

    @Column(length = 100)
    private String nationality;

    /**
     * Permanent car number (e.g., 1 for Verstappen, 44 for Hamilton).
     * Stored as String because some drivers have no permanent number.
     */
    @Column(name = "permanent_number", length = 5)
    private String permanentNumber;

    /** Three-letter driver code used in broadcasts (e.g., "VER", "HAM") */
    @Column(length = 5)
    private String code;

    /** Link to the driver's Wikipedia page, from Ergast data */
    @Column(length = 500)
    private String url;

    // ── Helper method ────────────────────────────────────────────────────────

    /** Returns full display name: "Max Verstappen" */
    public String getFullName() {
        return givenName + " " + familyName;
    }
}
