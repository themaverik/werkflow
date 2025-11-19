package com.werkflow.procurement.delegate;

import com.werkflow.procurement.entity.PoLineItem;
import com.werkflow.procurement.entity.PrLineItem;
import com.werkflow.procurement.entity.PurchaseOrder;
import com.werkflow.procurement.entity.PurchaseRequest;
import com.werkflow.procurement.repository.PoLineItemRepository;
import com.werkflow.procurement.repository.PrLineItemRepository;
import com.werkflow.procurement.repository.PurchaseOrderRepository;
import com.werkflow.procurement.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Delegate for creating Purchase Order from approved PR
 * This delegate handles LOCAL procurement operations only.
 */
@Slf4j
@Component("purchaseOrderCreationDelegate")
@RequiredArgsConstructor
public class PurchaseOrderCreationDelegate implements JavaDelegate {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PrLineItemRepository prLineItemRepository;
    private final PoLineItemRepository poLineItemRepository;

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

            // Copy line items from PR to PO
            copyLineItemsFromPrToPo(prId, savedPo.getId());

            // Update PR status to CONVERTED_TO_PO
            updatePurchaseRequestStatus(prId, "CONVERTED_TO_PO");

            log.info("Successfully created PO with line items and updated PR status");

        } catch (Exception e) {
            log.error("Error creating Purchase Order", e);
            throw new RuntimeException("Failed to create Purchase Order", e);
        }
    }

    private void copyLineItemsFromPrToPo(Long prId, Long poId) {
        log.debug("Copying line items from PR {} to PO {}", prId, poId);

        List<PrLineItem> prLineItems = prLineItemRepository.findByPurchaseRequestId(prId);

        for (PrLineItem prItem : prLineItems) {
            PoLineItem poItem = PoLineItem.builder()
                    .purchaseOrderId(poId)
                    .itemDescription(prItem.getItemDescription())
                    .quantity(prItem.getQuantity())
                    .unitPrice(prItem.getEstimatedUnitPrice())
                    .totalAmount(prItem.getEstimatedTotalAmount())
                    .notes(prItem.getNotes())
                    .build();

            poLineItemRepository.save(poItem);
            log.debug("Copied line item: {} - Quantity: {}", poItem.getItemDescription(), poItem.getQuantity());
        }

        log.info("Successfully copied {} line items from PR to PO", prLineItems.size());
    }

    private void updatePurchaseRequestStatus(Long prId, String newStatus) {
        log.debug("Updating PR {} status to {}", prId, newStatus);

        purchaseRequestRepository.findById(prId).ifPresent(pr -> {
            pr.setStatus(PurchaseRequest.PrStatus.valueOf(newStatus));
            purchaseRequestRepository.save(pr);
            log.info("Updated PR {} status to {}", prId, newStatus);
        });
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
