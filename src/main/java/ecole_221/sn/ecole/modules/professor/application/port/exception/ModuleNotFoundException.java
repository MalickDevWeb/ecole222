package ecole_221.sn.ecole.modules.professor.application.port.exception;

/**
 * Exception levée lorsque qu'un module n'est pas trouvé.
 */
public class ModuleNotFoundException extends RuntimeException {
    
    private final String moduleId;
    
    public ModuleNotFoundException(String moduleId) {
        super("Module non trouvé avec l'identifiant: " + moduleId);
        this.moduleId = moduleId;
    }
    
    public String getModuleId() {
        return moduleId;
    }
}
