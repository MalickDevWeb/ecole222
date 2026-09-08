package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.Permission;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AccessControlFactory {
    
    public static AccessControl createOwner(String resourceId, String resourceType, String userId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime grantedAt = LocalDateTime.now();
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.ADMIN);
        permissions.add(Permission.READ);
        permissions.add(Permission.WRITE);
        permissions.add(Permission.DELETE);
        permissions.add(Permission.VIEW_MESSAGES);
        permissions.add(Permission.SEND_MESSAGES);
        permissions.add(Permission.JOIN_MEET);
        permissions.add(Permission.HOST_MEET);
        permissions.add(Permission.MANAGE_PARTICIPANTS);
        permissions.add(Permission.SHARE_SCREEN);
        permissions.add(Permission.RECORD_MEET);
        
        return new AccessControl(id, resourceId, resourceType, userId, permissions, 
                              grantedAt, null, userId);
    }
    
    public static AccessControl createMember(String resourceId, String resourceType, String userId, 
                                            String grantedBy) {
        String id = UUID.randomUUID().toString();
        LocalDateTime grantedAt = LocalDateTime.now();
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.READ);
        permissions.add(Permission.VIEW_MESSAGES);
        permissions.add(Permission.SEND_MESSAGES);
        permissions.add(Permission.JOIN_MEET);
        
        return new AccessControl(id, resourceId, resourceType, userId, permissions, 
                              grantedAt, null, grantedBy);
    }
    
    public static AccessControl createViewer(String resourceId, String resourceType, String userId, 
                                            String grantedBy) {
        String id = UUID.randomUUID().toString();
        LocalDateTime grantedAt = LocalDateTime.now();
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.READ);
        permissions.add(Permission.VIEW_MESSAGES);
        permissions.add(Permission.JOIN_MEET);
        
        return new AccessControl(id, resourceId, resourceType, userId, permissions, 
                              grantedAt, null, grantedBy);
    }
    
    public static AccessControl createWithCustomPermissions(String resourceId, String resourceType, 
                                                          String userId, Set<Permission> permissions, 
                                                          String grantedBy) {
        String id = UUID.randomUUID().toString();
        LocalDateTime grantedAt = LocalDateTime.now();
        
        return new AccessControl(id, resourceId, resourceType, userId, permissions, 
                              grantedAt, null, grantedBy);
    }
    
    public static AccessControl reconstitute(String id, String resourceId, String resourceType, 
                                           String userId, Set<Permission> permissions, 
                                           LocalDateTime grantedAt, LocalDateTime revokedAt, 
                                           String grantedBy) {
        return new AccessControl(id, resourceId, resourceType, userId, permissions, 
                              grantedAt, revokedAt, grantedBy);
    }
}