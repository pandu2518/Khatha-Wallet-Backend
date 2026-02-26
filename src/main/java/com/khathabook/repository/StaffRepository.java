package com.khathabook.repository;

import com.khathabook.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByRetailerId(Long retailerId);
}
