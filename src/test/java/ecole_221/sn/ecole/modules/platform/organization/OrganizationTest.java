package ecole_221.sn.ecole.modules.platform.organization;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour Organization entity.
 * Valide les comportements métier de l'agrégat Organization.
 */
class OrganizationTest {
    
    @Test
    void testCreateOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        assertNotNull(org.getId());
        assertEquals("École Test", org.getName());
        assertEquals(OrganizationType.ECOLE, org.getType());
        assertEquals(OrganizationStatus.PENDING, org.getStatus());
        assertEquals("test@ecole.sn", org.getEmail());
        assertTrue(org.isPending());
        assertFalse(org.isActive());
    }
    
    @Test
    void testValidateOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        assertTrue(org.isPending());
        org.validate();
        
        assertTrue(org.isActive());
        assertFalse(org.isPending());
        assertNotNull(org.getValidatedAt());
    }
    
    @Test
    void testSuspendOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        org.validate();
        assertTrue(org.isActive());
        
        org.suspend();
        assertTrue(org.isSuspended());
        assertFalse(org.isActive());
        assertNotNull(org.getSuspendedAt());
    }
    
    @Test
    void testReactivateOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        org.validate();
        org.suspend();
        assertTrue(org.isSuspended());
        
        org.reactivate();
        assertTrue(org.isActive());
        assertFalse(org.isSuspended());
        assertNull(org.getSuspendedAt());
    }
    
    @Test
    void testCannotValidateNonPendingOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        org.validate();
        assertThrows(IllegalStateException.class, org::validate);
    }
    
    @Test
    void testCannotSuspendNonActiveOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        assertThrows(IllegalStateException.class, org::suspend);
    }
    
    @Test
    void testCannotReactivateNonSuspendedOrganization() {
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            "test@ecole.sn"
        );
        
        assertThrows(IllegalStateException.class, org::reactivate);
    }
    
    @Test
    void testOrganizationWithSettings() {
        Map<String, Object> settings = Map.of(
            "academicYearStart", "2026-09-01",
            "academicYearEnd", "2027-07-31",
            "currency", "XOF"
        );
        
        Organization org = OrganizationFactory.create(
            "École Test",
            OrganizationType.ECOLE,
            OrganizationStatus.ACTIVE,
            null, // slug
            "Senegal",
            "Dakar",
            "Test Address",
            "221334455",
            "test@ecole.sn",
            null,
            null,
            null,
            null,
            settings
        );
        
        assertEquals(settings, org.getSettings());
        assertEquals("XOF", org.getSettings().get("currency"));
    }
}
