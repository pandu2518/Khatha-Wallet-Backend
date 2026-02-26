package com.khathabook.repository;

import com.khathabook.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByRetailerId(Long retailerId);
    List<Attendance> findByStaffId(Long staffId);
    List<Attendance> findByRetailerIdAndDate(Long retailerId, LocalDate date);
    Attendance findByStaffIdAndDate(Long staffId, LocalDate date);
    List<Attendance> findByStaffIdAndDateBetween(Long staffId, LocalDate startDate, LocalDate endDate);
}
