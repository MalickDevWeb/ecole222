-- ============================================================
-- V13 : Billing & Entitlements (SaaS Plans & Limits)
-- ============================================================

CREATE TABLE IF NOT EXISTS plans (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price_monthly DECIMAL(10, 2) NOT NULL,
    price_yearly DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS entitlements (
    id VARCHAR(255) PRIMARY KEY,
    plan_id VARCHAR(255) NOT NULL,
    feature_code VARCHAR(255) NOT NULL,
    max_limit INT, -- NULL means unlimited
    is_enabled BOOLEAN DEFAULT TRUE,
    
    CONSTRAINT fk_entitlement_plan FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, TRIAL, PAST_DUE, SUSPENDED, CANCELED
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    next_billing_date TIMESTAMP,
    
    CONSTRAINT fk_sub_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE INDEX idx_subscriptions_org ON subscriptions(organization_id);

-- Insert Default Plans
INSERT INTO plans (id, name, description, price_monthly, price_yearly) VALUES
('plan-free', 'Basic Free', 'Plan gratuit de base', 0.00, 0.00),
('plan-pro', 'Professional', 'Plan pro pour écoles moyennes', 49.99, 499.99),
('plan-enterprise', 'Enterprise', 'Plan illimité', 199.99, 1999.99);

-- Insert basic entitlements (max_users, max_storage_gb)
INSERT INTO entitlements (id, plan_id, feature_code, max_limit) VALUES
('ent-free-users', 'plan-free', 'MAX_USERS', 50),
('ent-pro-users', 'plan-pro', 'MAX_USERS', 500),
('ent-ent-users', 'plan-enterprise', 'MAX_USERS', NULL);

-- Create a subscription for the Default Tenant
DO $$
DECLARE
    default_org_id VARCHAR(255);
BEGIN
    SELECT id INTO default_org_id FROM organizations ORDER BY created_at ASC LIMIT 1;
    
    IF default_org_id IS NOT NULL THEN
        INSERT INTO subscriptions (id, organization_id, plan_id, status, start_date)
        VALUES ('sub-default', default_org_id, 'plan-enterprise', 'ACTIVE', NOW());
        
        -- Update the organization to point to this subscription
        -- We added subscription_id in V10 (or it was mapped in Java)
        -- Note: Let's assume we can update it if the column exists
    END IF;
END $$;

