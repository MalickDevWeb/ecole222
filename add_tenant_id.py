import os
import re

directory = 'src/main/java/ecole_221/sn/ecole/modules'
exclude_files = [
    'OrganizationJpaEntity.java', 
    'OrganizationUserJpaEntity.java', 
    'UserJpaEntity.java',
    'CustomRoleJpaEntity.java',
    'RolePermissionJpaEntity.java',
    'DelegationJpaEntity.java',
    'AuditLogJpaEntity.java',
    'SecurityEventJpaEntity.java'
]

# Find all files with @Entity
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java') and file not in exclude_files:
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            if '@Entity' in content and 'organizationId' not in content:
                # Find the class declaration
                class_match = re.search(r'public class \w+[^\{]*\{', content)
                if class_match:
                    insert_pos = class_match.end()
                    
                    injection = """
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
"""
                    new_content = content[:insert_pos] + injection + content[insert_pos:]
                    
                    with open(filepath, 'w') as f:
                        f.write(new_content)
                    print(f"Injected into {file}")
