package com.leaseflow.service;

import com.leaseflow.entity.Payment;
import com.leaseflow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Page<Payment> findByTenant(Long tenantId, Pageable pageable) {
        return paymentRepository.findByTenantId(tenantId, pageable);
    }

    public List<Payment> findTop5ByTenant(Long tenantId) {
        return paymentRepository.findTop5ByTenantId(tenantId, PageRequest.of(0, 5));
    }
}

