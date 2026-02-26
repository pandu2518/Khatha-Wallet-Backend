package com.khathabook.controller;

import com.khathabook.model.Attendance;
import com.khathabook.model.Staff;
import com.khathabook.repository.AttendanceRepository;
import com.khathabook.repository.StaffRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "*")
public class StaffController {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @GetMapping
    public ResponseEntity<?> getStaff(@RequestParam Long retailerId) {
        return ResponseEntity.ok(staffRepository.findByRetailerId(retailerId));
    }

    @PostMapping
    public ResponseEntity<?> addStaff(@RequestBody Staff staff, @RequestParam Long retailerId) {
        staff.setRetailerId(retailerId);
        if (staff.getJoiningDate() == null) staff.setJoiningDate(LocalDate.now());
        return ResponseEntity.ok(staffRepository.save(staff));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id, @RequestParam Long retailerId) {
        staffRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<?> getTodayAttendance(@RequestParam Long retailerId) {
        return ResponseEntity.ok(attendanceRepository.findByRetailerIdAndDate(retailerId, LocalDate.now()));
    }

    @PostMapping("/attendance")
    public ResponseEntity<?> markAttendance(@RequestBody Attendance attendance, @RequestParam Long retailerId) {
        
        Staff staff = staffRepository.findById(attendance.getStaffId()).orElse(null);
        if (staff == null) return ResponseEntity.badRequest().body("Staff not found");

        Attendance existing = attendanceRepository.findByStaffIdAndDate(attendance.getStaffId(), LocalDate.now());
        if (existing != null) {
            existing.setStatus(attendance.getStatus());
            existing.setNotes(attendance.getNotes());
            existing.setCheckInTime(attendance.getCheckInTime());
            existing.setCheckOutTime(attendance.getCheckOutTime());
            
            // Calculate daily earning based on daily wage
            if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                existing.setDailyEarning(staff.getDailyWage());
            } else if (attendance.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {
                existing.setDailyEarning(staff.getDailyWage() / 2);
            } else {
                existing.setDailyEarning(0.0);
            }
            
            return ResponseEntity.ok(attendanceRepository.save(existing));
        }

        attendance.setRetailerId(retailerId);
        attendance.setDate(LocalDate.now());
        
        if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
            attendance.setDailyEarning(staff.getDailyWage());
        } else if (attendance.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {
            attendance.setDailyEarning(staff.getDailyWage() / 2);
        } else {
            attendance.setDailyEarning(0.0);
        }

        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }
    
    @GetMapping("/{staffId}/attendance")
    public ResponseEntity<?> getStaffAttendance(@PathVariable Long staffId, @RequestParam Long retailerId) {
        return ResponseEntity.ok(attendanceRepository.findByStaffId(staffId));
    }

    @GetMapping("/{staffId}/attendance/monthly")
    public ResponseEntity<?> getMonthlyAttendance(
            @PathVariable Long staffId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam Long retailerId) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return ResponseEntity.ok(attendanceRepository.findByStaffIdAndDateBetween(staffId, startDate, endDate));
    }
}
