package com.smartwater.repo;

import com.smartwater.domain.WaterUsageLog;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface UsageRepository extends JpaRepository<WaterUsageLog, Long> {
    boolean existsByHouseholdIdAndReadingDate(Long householdId, LocalDate date);
    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(Long householdId, LocalDate from, LocalDate to);

    @Query("select sum(l.litres) from WaterUsageLog l where l.household.id = :hid and l.readingDate between :from and :to")
    BigDecimal sumLitres(@Param("hid") Long householdId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** rows of [householdId (Long), totalLitres (BigDecimal)] */
    @Query("select l.household.id, sum(l.litres) from WaterUsageLog l where l.household.apartment.id = :aid and l.readingDate between :from and :to group by l.household.id")
    List<Object[]> totalsByHousehold(@Param("aid") Long apartmentId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
