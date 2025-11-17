-- Flyway Migration V1: Create Procurement Service Initial Schema
-- This script creates all tables for the Procurement management system

-- Create Vendors table
CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    vendor_code VARCHAR(50) NOT NULL UNIQUE,
    vendor_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    contact_person VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    website VARCHAR(100),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    tax_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    rating NUMERIC(3, 2),
    total_purchases NUMERIC(15, 2) DEFAULT 0,
    payment_terms VARCHAR(255),
    delivery_lead_time_days INTEGER,
    minimum_order_amount NUMERIC(15, 2),
    approved_by VARCHAR(255),
    approved_at DATE,
    last_purchase_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_vendor_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_APPROVAL', 'BLACKLISTED', 'SUSPENDED', 'PROBATION'))
);

-- Create indexes for Vendors
CREATE INDEX idx_vendor_code ON vendors(vendor_code);
CREATE INDEX idx_vendor_status ON vendors(status);
CREATE INDEX idx_vendor_name ON vendors(vendor_name);
CREATE INDEX idx_vendor_email ON vendors(email);
CREATE INDEX idx_vendor_city ON vendors(city);
CREATE INDEX idx_vendor_country ON vendors(country);

-- Create Purchase Requests table
CREATE TABLE purchase_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    quantity NUMERIC(15, 2) NOT NULL,
    unit VARCHAR(50),
    estimated_unit_price NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    requested_by VARCHAR(255) NOT NULL,
    request_date DATE NOT NULL,
    required_by_date DATE NOT NULL,
    department_name VARCHAR(255),
    business_justification VARCHAR(500),
    approved_by VARCHAR(255),
    approved_at DATE,
    rejection_reason VARCHAR(500),
    rejected_by VARCHAR(255),
    rejected_at DATE,
    rfq_sent_at DATE,
    preferred_vendor_id BIGINT,
    selected_vendor_id BIGINT,
    purchase_order_id BIGINT,
    workflow_instance_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_pr_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'RFQ_SENT', 'QUOTES_RECEIVED', 'VENDOR_SELECTED', 'PO_CREATED', 'PO_SENT', 'PO_ACKNOWLEDGED', 'IN_DELIVERY', 'RECEIVED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_pr_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT fk_pr_preferred_vendor FOREIGN KEY (preferred_vendor_id) REFERENCES vendors(id) ON DELETE SET NULL,
    CONSTRAINT fk_pr_selected_vendor FOREIGN KEY (selected_vendor_id) REFERENCES vendors(id) ON DELETE SET NULL
);

-- Create indexes for Purchase Requests
CREATE INDEX idx_pr_number ON purchase_requests(request_number);
CREATE INDEX idx_pr_status ON purchase_requests(status);
CREATE INDEX idx_pr_requested_by ON purchase_requests(requested_by);
CREATE INDEX idx_pr_department ON purchase_requests(department_name);
CREATE INDEX idx_pr_priority ON purchase_requests(priority);
CREATE INDEX idx_pr_preferred_vendor ON purchase_requests(preferred_vendor_id);
CREATE INDEX idx_pr_selected_vendor ON purchase_requests(selected_vendor_id);

-- Create Purchase Orders table
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    po_number VARCHAR(50) NOT NULL UNIQUE,
    purchase_request_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    quantity NUMERIC(15, 2) NOT NULL,
    unit VARCHAR(50),
    unit_price NUMERIC(15, 2) NOT NULL,
    po_amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    po_date DATE NOT NULL,
    delivery_date DATE NOT NULL,
    delivery_address VARCHAR(255),
    payment_terms VARCHAR(255),
    special_instructions VARCHAR(500),
    created_by_user VARCHAR(255),
    sent_date DATE,
    acknowledged_date DATE,
    delivery_received_date DATE,
    invoice_received_date DATE,
    payment_date DATE,
    po_closed_date DATE,
    cancellation_reason VARCHAR(500),
    workflow_instance_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_po_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'SENT', 'ACKNOWLEDGED', 'IN_FULFILLMENT', 'IN_DELIVERY', 'RECEIVED', 'INVOICED', 'PAID', 'CLOSED', 'CANCELLED')),
    CONSTRAINT fk_po_purchase_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_po_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT
);

-- Create indexes for Purchase Orders
CREATE INDEX idx_po_number ON purchase_orders(po_number);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_vendor_id ON purchase_orders(vendor_id);
CREATE INDEX idx_po_request_id ON purchase_orders(purchase_request_id);
CREATE INDEX idx_po_date ON purchase_orders(po_date);
CREATE INDEX idx_po_delivery_date ON purchase_orders(delivery_date);
