package com.werkflow.procurement.delegate;

import com.werkflow.procurement.entity.PurchaseOrder;
import com.werkflow.procurement.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Delegate for creating Purchase Order from approved PR
 */
@Slf4j
@Component("purchaseOrderCreationDelegate")
@RequiredArgsConstructor
public class PurchaseOrderCreationDelegate implements JavaDelegate {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing PurchaseOrderCreationDelegate - Process Instance: {}",
                 execution.getProcessInstanceId());

        try {
            Long prId = (Long) execution.getVariable("prId");
            Long vendorId = (Long) execution.getVariable("selectedVendorId");
            String deliveryAddress = (String) execution.getVariable("deliveryAddress");

            log.info("Creating PO for PR: {}, Vendor: {}", prId, vendorId);

            // Create Purchase Order
            PurchaseOrder po = PurchaseOrder.builder()
                    .poNumber(generatePONumber())
                    .vendorId(vendorId)
                    .status(PurchaseOrder.PoStatus.DRAFT)
                    .orderDate(LocalDate.now())
                    .expectedDeliveryDate(calculateExpectedDelivery())
                    .deliveryAddress(deliveryAddress)
                    .processInstanceId(execution.getProcessInstanceId())
                    .build();

            PurchaseOrder savedPo = purchaseOrderRepository.save(po);

            log.info("Purchase Order created successfully - PO Number: {}, ID: {}",
                     savedPo.getPoNumber(), savedPo.getId());

            // Set PO details as process variables
            execution.setVariable("poId", savedPo.getId());
            execution.setVariable("poNumber", savedPo.getPoNumber());

            // TODO: Copy line items from PR to PO
            // TODO: Update PR status to CONVERTED_TO_PO

        } catch (Exception e) {
            log.error("Error creating Purchase Order", e);
            throw new RuntimeException("Failed to create Purchase Order", e);
        }
    }

    private String generatePONumber() {
        // Generate sequential PO number
        // Format: PO-YYYYMMDD-NNNN
        LocalDate today = LocalDate.now();
        long count = purchaseOrderRepository.count() + 1;
        return String.format("PO-%04d%02d%02d-%04d",
                           today.getYear(), today.getMonthValue(), today.getDayOfMonth(), count);
    }

    private LocalDate calculateExpectedDelivery() {
        // Default: 14 days from now
        return LocalDate.now().plusDays(14);
    }
}
