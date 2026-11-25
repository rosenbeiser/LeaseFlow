package com.leaseflow.service;

import com.leaseflow.entity.MeterReading;
import com.leaseflow.entity.Unit;
import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.enums.UtilityType;
import com.leaseflow.exception.BusinessRuleException;
import com.leaseflow.exception.ResourceNotFoundException;
import com.leaseflow.repository.MeterReadingRepository;
import com.leaseflow.repository.UnitRepository;
import com.leaseflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;

    public Page<MeterReading> findByUnit(Long unitId, Long userId, Pageable pageable) {
        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ROLE_ADMIN).orElse(false);

        if (!isAdmin) {
            // Verify ownership
            unitRepository.findByIdAndPropertyLandlordId(unitId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unit not found or you do not have access to it."));
        } else {
            unitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));
        }

        return meterReadingRepository.findByUnitId(unitId, pageable);
    }

    @Transactional
    public MeterReading record(MeterReading reading, Long userId) {
        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() == Role.ROLE_ADMIN).orElse(false);

        Unit unit;
        if (!isAdmin) {
            unit = unitRepository.findByIdAndPropertyLandlordId(
                            reading.getUnit().getId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unit not found or you do not have access to it."));
        } else {
            unit = unitRepository.findById(reading.getUnit().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", reading.getUnit().getId()));
        }
        reading.setUnit(unit);

        // Auto-derive previousReading if not provided or zero
        if (reading.getPreviousReading() == null) {
            BigDecimal prev = getLatestReading(unit.getId(), reading.getUtilityType(),
                    reading.getReadingDate().toString());
            reading.setPreviousReading(prev);
        }

        // Validate currentReading >= previousReading
        if (reading.getCurrentReading().compareTo(reading.getPreviousReading()) < 0) {
            throw new BusinessRuleException(
                    "Current reading (" + reading.getCurrentReading() +
                    ") cannot be less than previous reading (" + reading.getPreviousReading() + ").");
        }

        return meterReadingRepository.save(reading);
    }

    public BigDecimal getLatestReading(Long unitId, UtilityType type, String beforeDateStr) {
        List<MeterReading> list = meterReadingRepository.findLatestBefore(
                unitId, type, java.time.LocalDate.parse(beforeDateStr), PageRequest.of(0, 1));
        return list.isEmpty() ? BigDecimal.ZERO : list.get(0).getCurrentReading();
    }
}
