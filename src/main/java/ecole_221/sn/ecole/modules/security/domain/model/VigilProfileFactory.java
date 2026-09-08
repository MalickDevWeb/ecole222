package ecole_221.sn.ecole.modules.security.domain.model;

import ecole_221.sn.ecole.modules.security.domain.valueobject.ProfileStatus;
import ecole_221.sn.ecole.modules.security.domain.valueobject.SecurityRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class VigilProfileFactory {
    
    public static VigilProfile create(String name, String userId, SecurityRole role, ProfileStatus status) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();

        return VigilProfile.createInternal(id, name, role.role(), userId, status.status(), createdAt.toString());
    }

    public static VigilProfile createDefault(String name, String userId) {
        return create(name, userId, SecurityRole.VIGIL, ProfileStatus.ACTIVE);
    }

    public static VigilProfile reconstitute(String id, String name, String role, String userId,
                                           String status, String createdAt) {
        return VigilProfile.createInternal(id, name, role, userId, status, createdAt);
    }
}