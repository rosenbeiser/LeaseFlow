package com.leaseflow.service;

import com.leaseflow.entity.Invoice;
import com.leaseflow.entity.Lease;
import com.leaseflow.entity.Payment;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.enums.Role;
import com.leaseflow.enums.UnitStatus;
import com.leaseflow.repository.LeaseRepository;
import com.leaseflow.repository.PropertyRepository;
import com.leaseflow.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final LeaseRepository leaseRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final LeaseService leaseService;
    private final UserService userService;

    public Map<String, Object> getLandlordDashboard(Long userId) {
        User user = userService.findById(userId);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        Map<String, Object> data = new LinkedHashMap<>();

        long totalUnits    = isAdmin ? unitRepository.count() : unitRepository.countByLandlordId(userId);
        long occupiedUnits = isAdmin ? unitRepository.countByStatus(UnitStatus.OCCUPIED) : unitRepository.countByLandlordIdAndStatus(userId, UnitStatus.OCCUPIED);
        long vacantUnits   = totalUnits - occupiedUnits;
        double occupancyPct = totalUnits > 0 ? (occupiedUnits * 100.0 / totalUnits) : 0.0;

        data.put("propertyCount",       isAdmin ? propertyRepository.count() : propertyRepository.countByLandlordId(userId));
        data.put("totalUnits",           totalUnits);
        data.put("occupiedUnits",        occupiedUnits);
        data.put("vacantUnits",          vacantUnits);
        data.put("occupancyPercentage",  Math.round(occupancyPct * 100.0) / 100.0);
        data.put("activeLeaseCount",     isAdmin ? leaseRepository.countByStatus(LeaseStatus.ACTIVE) : leaseRepository.countByLandlordIdAndStatus(userId, LeaseStatus.ACTIVE));
        data.put("outstandingBalance",   isAdmin ? invoiceService.sumAllOutstanding() : invoiceService.sumOutstandingByLandlord(userId));
        data.put("overdueInvoiceCount",  isAdmin ? invoiceService.countAllOverdue() : invoiceService.countOverdueByLandlord(userId));

        // Revenue chart: last 6 months
        YearMonth now = YearMonth.now();
        int fromMonth = InvoiceService.toMonthInt(now.minusMonths(5));
        List<Object[]> raw = isAdmin ? invoiceService.revenueByMonthPlatform(fromMonth) : invoiceService.revenueByMonth(userId, fromMonth);
        List<Map<String, Object>> revenue = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", InvoiceService.fromMonthInt((Integer) row[0]).toString());
            entry.put("collected", row[1]);
            revenue.add(entry);
        }
        data.put("revenueByMonth", revenue);

        // 5 most recent invoices
        List<Invoice> recent = isAdmin ? invoiceService.getRecentInvoicesPlatform() : invoiceService.getRecentInvoicesForLandlord(userId);
        data.put("recentInvoices", recent);

        return data;
    }

    public Map<String, Object> getTenantDashboard(Long tenantId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // Current active lease
        List<Lease> leases = leaseService.findAllByTenant(tenantId);
        Lease activeLease = leases.stream()
                .filter(l -> l.getStatus() == LeaseStatus.ACTIVE
                        || l.getStatus() == LeaseStatus.NOTICE_SERVED)
                .findFirst().orElse(null);
        data.put("currentLease", activeLease);

        // Next due invoice
        Invoice nextDue = invoiceService.findNextDueForTenant(tenantId);
        data.put("nextDueInvoice", nextDue);

        // Outstanding balance
        data.put("outstandingBalance", invoiceService.sumOutstandingByTenant(tenantId));

        // Last 5 payments
        List<Payment> recentPayments = paymentService.findTop5ByTenant(tenantId);
        data.put("recentPayments", recentPayments);

        return data;
    }
}
