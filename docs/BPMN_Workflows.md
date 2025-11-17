# BPMN Workflow Documentation

This document provides an overview of all implemented BPMN workflows in the Werkflow Enterprise Platform.

## Overview

The Werkflow platform uses Flowable BPM engine to orchestrate complex business processes across Finance, Procurement, and Inventory services. All workflows are defined in BPMN 2.0 XML format.

## Finance Service Workflows

### CapEx Approval Workflow

**Process ID**: `capexApproval`
**File**: `services/finance/src/main/resources/processes/capex-approval.bpmn20.xml`
**Form**: `services/finance/src/main/resources/forms/capex-request-form.json`

**Description**: Multi-level approval workflow for capital expenditure requests with amount-based routing.

**Process Flow**:
1. Budget availability check
2. Department head approval
3. Amount-based routing:
   - < $10K: CFO approval
   - $10K - $100K: CFO + CEO approval
   - > $100K: CFO + CEO + Board approval
4. Legal contract review (if > $100K)
5. Vendor selection and RFQ process
6. Purchase order creation
7. Goods receipt
8. Invoice processing and payment
9. Asset registration

**Key Features**:
- Automatic budget validation
- Multi-tier approval based on expenditure amount
- Integration with Legal service for contract review
- Integration with Procurement service for vendor selection
- Asset tracking and registration

**Delegate Classes**:
- `BudgetAvailabilityDelegate`: Checks budget availability
- `NotificationDelegate`: Sends email and system notifications

---

## Procurement Service Workflows

### 1. PR to PO Workflow

**Process ID**: `prToPo`
**File**: `services/procurement/src/main/resources/processes/pr-to-po.bpmn20.xml`

**Description**: Converts approved Purchase Requisitions into Purchase Orders.

**Process Flow**:
1. Budget check via Finance Service
2. Manager approval
3. Procurement review
4. Vendor selection (triggers RFQ if needed)
5. PO creation
6. Send PO to vendor
7. Wait for vendor acknowledgment (48-hour timeout)

**Key Features**:
- Cross-service budget validation
- Automatic RFQ triggering for vendor selection
- Vendor acknowledgment tracking with timeout handling

**Delegate Classes**:
- `FinanceBudgetCheckDelegate`: Calls Finance Service API
- `PurchaseOrderCreationDelegate`: Creates PO entity

### 2. Vendor Onboarding Workflow

**Process ID**: `vendorOnboarding`
**File**: `services/procurement/src/main/resources/processes/vendor-onboarding.bpmn20.xml`

**Description**: Complete vendor registration and approval process.

**Process Flow**:
1. Initial screening by procurement
2. Document verification
3. Parallel reviews:
   - Financial background check (Finance Service)
   - Legal contract terms review
   - Compliance check
4. Procurement manager approval
5. Vendor activation in system
6. Vendor portal account creation
7. Welcome email

**Key Features**:
- Parallel review execution for efficiency
- Multi-department collaboration
- Automated compliance verification

### 3. RFQ Process Workflow

**Process ID**: `rfqProcess`
**File**: `services/procurement/src/main/resources/processes/rfq-process.bpmn20.xml`

**Description**: Request for Quotation process with vendor evaluation.

**Process Flow**:
1. Vendor selection (minimum 3 vendors)
2. RFQ distribution
3. Quote collection (with deadline)
4. Evaluation committee review (multi-instance)
5. Quote scoring and ranking
6. Final vendor selection
7. Contract negotiation (if needed)
8. RFQ award and PO creation

**Key Features**:
- Minimum vendor count validation
- Deadline enforcement with extension capability
- Multi-instance parallel evaluation
- Automated scoring and ranking

### 4. Goods Receipt Workflow

**Process ID**: `goodsReceipt`
**File**: `services/procurement/src/main/resources/processes/goods-receipt.bpmn20.xml`

**Description**: Complete goods receipt process with 3-way matching.

**Process Flow**:
1. Physical inspection
2. Discrepancy handling (accept partial/reject)
3. 3-way matching (PO-GR-Invoice)
4. Inventory update
5. Quality inspection (if required)
6. PO status update
7. Invoice processing trigger
8. GRN generation

**Key Features**:
- Physical inspection and quality control
- 3-way matching validation
- Partial acceptance support
- Integration with Inventory Service

---

## Inventory Service Workflows

### 1. Stock Requisition Workflow

**Process ID**: `stockRequisition`
**File**: `services/inventory/src/main/resources/processes/stock-requisition.bpmn20.xml`

**Description**: Internal stock requisition and issuance process.

**Process Flow**:
1. Requisition validation
2. Stock availability check
3. Manager approval (for high-value items)
4. Stock reservation
5. Item picking by warehouse staff
6. Verification by supervisor
7. Inventory update
8. Stock issue note generation
9. Handover to requester

**Key Features**:
- Automatic procurement triggering for out-of-stock items
- Approval routing based on item value
- Picking verification workflow

**Delegate Classes**:
- `InventoryAvailabilityDelegate`: Checks stock across hubs

### 2. E-commerce Order Fulfillment Workflow

**Process ID**: `orderFulfillment`
**File**: `services/inventory/src/main/resources/processes/order-fulfillment.bpmn20.xml`

**Description**: Order fulfillment with batch hub assignment (Critical Feature).

**Process Flow**:
1. Order validation
2. Item availability check across all hubs
3. Tentative stock reservation
4. Order confirmation to customer
5. **Wait for batch hub assignment (12 PM or 6 PM)**
6. Hub assignment validation
7. Finalize reservation at assigned hub
8. Pick, QC, and pack at hub
9. Dispatch with tracking
10. Delivery confirmation

**Key Features**:
- **Batch hub assignment optimization** (runs at 12 PM and 6 PM)
- Tentative reservations before hub assignment
- Hub-specific task routing
- Delivery tracking and timeout handling

**Delegate Classes**:
- `ReservationDelegate`: Creates tentative reservations
- `InventoryAvailabilityDelegate`: Checks multi-hub availability

---

## Batch Processing

### Hub Assignment Batch Job

**Schedule**: 12:00 PM and 6:00 PM daily

**Process**:
1. Collect all orders waiting for hub assignment
2. For each order:
   - Extract customer delivery address
   - Find hubs with all required items in stock
   - Calculate distance from customer to each hub
   - Apply hub assignment algorithm (nearest hub with stock)
3. Assign hub and trigger workflow continuation
4. Send `hubAssigned` message event to workflow instances

**Algorithm Reference**: See `docs/Enterprise_Workflow_Roadmap.md` lines 1350-1573

---

## Integration Points

### Finance ↔ Procurement
- Budget validation API calls
- CapEx to PO conversion
- Invoice processing triggers

### Procurement ↔ Inventory
- Goods receipt to stock update
- PO delivery tracking

### Inventory ↔ E-commerce
- Real-time stock availability
- Order fulfillment tracking

---

## Form Definitions

All user tasks reference dynamic forms defined in JSON schema format:

- **CapEx Request Form**: `services/finance/src/main/resources/forms/capex-request-form.json`
- Additional forms: To be created for each user task

---

## Deployment

BPMN workflows are automatically deployed when the service starts. Flowable scans the `processes/` directory and deploys all `.bpmn20.xml` files.

### Configuration

Add to `application.yml`:

```yaml
flowable:
  process:
    definition-cache-limit: 100
  database-schema-update: true
  async-executor-activate: true
```

---

## Testing

### Unit Testing
Use Flowable's test framework to test workflows:

```java
@ExtendWith(FlowableExtension.class)
class CapExWorkflowTest {

    @Test
    void testCapExApprovalFlow(ProcessEngine processEngine) {
        // Start process instance
        // Complete user tasks
        // Assert process completed successfully
    }
}
```

### Integration Testing
Test complete end-to-end flows with all services running.

---

## Monitoring

Monitor workflows using:
- Flowable Admin Console: http://localhost:8080/flowable-admin
- Flowable Task: http://localhost:8080/flowable-task
- Process instance metrics in application logs

---

## Next Steps

1. Implement remaining delegate classes
2. Create form definitions for all user tasks
3. Set up Kafka event publishers for cross-service communication
4. Implement hub assignment batch job
5. Create workflow monitoring dashboard

---

## References

- Flowable Documentation: https://flowable.com/open-source/docs/
- BPMN 2.0 Specification: https://www.omg.org/spec/BPMN/2.0/
- Enterprise Roadmap: `docs/Enterprise_Workflow_Roadmap.md`
