package com.leaseflow.service;

import com.leaseflow.entity.Property;
import com.leaseflow.entity.Unit;
import com.leaseflow.enums.UnitStatus;
import com.leaseflow.exception.DuplicateResourceException;
import com.leaseflow.exception.ResourceNotFoundException;
import com.leaseflow.repository.PropertyRepository;
import com.leaseflow.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;

    public Page<Property> findByLandlord(Long landlordId, Pageable pageable) {
        return propertyRepository.findByLandlordId(landlordId, pageable);
    }

    public Page<Property> findAll(Pageable pageable) {
        return propertyRepository.findAll(pageable);
    }

    public Property findByIdAndLandlord(Long id, Long landlordId) {
        return propertyRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property not found or you do not have access to it."));
    }

    public Property findById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }

    public List<Unit> findUnitsByProperty(Long propertyId, Long landlordId) {
        // Verify ownership
        findByIdAndLandlord(propertyId, landlordId);
        return unitRepository.findByPropertyId(propertyId);
    }

    public List<Unit> findUnitsByPropertyAdmin(Long propertyId) {
        findById(propertyId);
        return unitRepository.findByPropertyId(propertyId);
    }

    @Transactional
    public Property createProperty(Property property, List<Unit> units) {
        // Validate unit numbers are unique within this new property
        long distinctCount = units.stream()
                .map(Unit::getUnitNumber)
                .distinct().count();
        if (distinctCount < units.size()) {
            throw new DuplicateResourceException("Duplicate unit numbers within the same property.");
        }

        Property saved = propertyRepository.save(property);
        for (Unit unit : units) {
            unit.setProperty(saved);
            unit.setStatus(UnitStatus.VACANT);
        }
        saved.getUnits().addAll(units);
        return propertyRepository.save(saved);
    }

    public Unit findUnitById(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));
    }

    public Unit findUnitByIdAndLandlord(Long unitId, Long landlordId) {
        return unitRepository.findByIdAndPropertyLandlordId(unitId, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unit not found or you do not have access to it."));
    }

    @Transactional
    public Unit saveUnit(Unit unit) {
        // Enforce unit-number uniqueness within property
        if (unitRepository.existsByUnitNumberAndPropertyId(
                unit.getUnitNumber(), unit.getProperty().getId())) {
            throw new DuplicateResourceException(
                    "Unit number '" + unit.getUnitNumber() + "' already exists in this property.");
        }
        return unitRepository.save(unit);
    }
}
