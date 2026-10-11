package ecole_221.sn.ecole.shared.infrastructure.tenant;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour TenantContext.
 * Valide le comportement thread-safe du contexte multi-tenant.
 */
class TenantContextTest {
    
    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    void testSetAndGetOrganization() {
        Organization org = createTestOrganization("org-1");
        
        TenantContext.setCurrentOrganization(org);
        
        assertTrue(TenantContext.getCurrentOrganization().isPresent());
        assertEquals("org-1", TenantContext.getCurrentOrganizationId().orElse(null));
    }
    
    @Test
    void testClearOrganization() {
        Organization org = createTestOrganization("org-1");
        
        TenantContext.setCurrentOrganization(org);
        assertTrue(TenantContext.hasTenant());
        
        TenantContext.clear();
        assertFalse(TenantContext.hasTenant());
        assertFalse(TenantContext.getCurrentOrganization().isPresent());
    }
    
    @Test
    void testPlatformMode() {
        assertFalse(TenantContext.isPlatformMode());
        
        TenantContext.setPlatformMode(true);
        assertTrue(TenantContext.isPlatformMode());
        
        TenantContext.setPlatformMode(false);
        assertFalse(TenantContext.isPlatformMode());
    }
    
    @Test
    void testHasTenant() {
        assertFalse(TenantContext.hasTenant());
        
        Organization org = createTestOrganization("org-1");
        TenantContext.setCurrentOrganization(org);
        
        assertTrue(TenantContext.hasTenant());
    }
    
    @Test
    void testWithTenantAsync() {
        Organization org = createTestOrganization("org-1");
        
        CompletableFuture<String> future = TenantContext.withTenant(org, () -> {
            return TenantContext.getCurrentOrganizationId().orElse(null);
        });
        
        String result = future.join();
        assertEquals("org-1", result);
    }
    
    @Test
    void testInPlatformModeAsync() {
        CompletableFuture<Boolean> future = TenantContext.inPlatformMode(() -> {
            return TenantContext.isPlatformMode();
        });
        
        Boolean result = future.join();
        assertTrue(result);
    }
    
    @Test
    void testThreadIsolation() throws InterruptedException {
        Organization org1 = createTestOrganization("org-1");
        Organization org2 = createTestOrganization("org-2");
        
        Thread thread1 = new Thread(() -> {
            TenantContext.setCurrentOrganization(org1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals("org-1", TenantContext.getCurrentOrganizationId().orElse(null));
        });
        
        Thread thread2 = new Thread(() -> {
            TenantContext.setCurrentOrganization(org2);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals("org-2", TenantContext.getCurrentOrganizationId().orElse(null));
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // Le thread principal ne doit pas avoir de contexte
        assertFalse(TenantContext.hasTenant());
    }
    
    @Test
    void testRequiresTenant() {
        assertTrue(TenantContext.requiresTenant());
        
        TenantContext.setPlatformMode(true);
        assertFalse(TenantContext.requiresTenant());
        
        TenantContext.setPlatformMode(false);
        assertTrue(TenantContext.requiresTenant());
        
        Organization org = createTestOrganization("org-1");
        TenantContext.setCurrentOrganization(org);
        assertFalse(TenantContext.requiresTenant());
    }
    
    private Organization createTestOrganization(String id) {
        return OrganizationFactory.reconstruct(
            id,
            "Test Organization",
            "ECOLE",
            "ACTIVE",
            null, // slug
            "Senegal",
            "Dakar",
            "Test Address",
            "221334455",
            "test@example.com",
            null, // website
            null, // logo
            null, // subscriptionId
            null, // primaryDomainId
            Map.of(), // settings
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            null,
            null
        );
    }
}
