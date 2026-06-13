package com.example.demo.repository;

import com.example.demo.model.Doubt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoubtRepository extends JpaRepository<Doubt, Long> {
    List<Doubt> findByCategory(String category);
    List<Doubt> findByStatus(String status);
    List<Doubt> findByCategoryAndStatus(String category, String status);
}
