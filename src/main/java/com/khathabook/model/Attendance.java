package com.khathabook.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long staffId;
    private LocalDate date;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    private Long retailerId;
    private Double dailyEarning;
    private String notes;

    public enum AttendanceStatus {
        PRESENT, ABSENT, HALF_DAY, LEAVE
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public Long getRetailerId() { return retailerId; }
    public void setRetailerId(Long retailerId) { this.retailerId = retailerId; }

    public Double getDailyEarning() { return dailyEarning; }
    public void setDailyEarning(Double dailyEarning) { this.dailyEarning = dailyEarning; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
