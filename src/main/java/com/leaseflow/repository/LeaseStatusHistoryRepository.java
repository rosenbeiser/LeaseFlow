package com.leaseflow.repository;

import com.leaseflow.entity.LeaseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaseStatusHistoryRepository extends JpaRepository<LeaseStatusHistory, Long> {

    List<LeaseStatusHistory> findByLeaseIdOrderByChangedAtAsc(Long leaseId);
}

