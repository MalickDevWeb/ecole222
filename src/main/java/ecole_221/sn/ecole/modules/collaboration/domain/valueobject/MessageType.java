package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

public record MessageType(String type) {
    public static final MessageType TEXT = new MessageType("TEXT");
    public static final MessageType AUDIO = new MessageType("AUDIO");
    public static final MessageType IMAGE = new MessageType("IMAGE");
    public static final MessageType VIDEO = new MessageType("VIDEO");
    public static final MessageType FILE = new MessageType("FILE");
    public static final MessageType SYSTEM = new MessageType("SYSTEM");
    
    public MessageType {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Le type de message ne peut pas être vide");
        }
    }
    
    public boolean isText() {
        return "TEXT".equals(type);
    }
    
    public boolean isAudio() {
        return "AUDIO".equals(type);
    }
    
    public boolean isImage() {
        return "IMAGE".equals(type);
    }
    
    public boolean isVideo() {
        return "VIDEO".equals(type);
    }
    
    public boolean isFile() {
        return "FILE".equals(type);
    }
    
    public boolean isSystem() {
        return "SYSTEM".equals(type);
    }
    
    public boolean isMedia() {
        return isAudio() || isImage() || isVideo() || isFile();
    }
}