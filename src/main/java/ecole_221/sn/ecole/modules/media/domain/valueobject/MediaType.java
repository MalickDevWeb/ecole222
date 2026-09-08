package ecole_221.sn.ecole.modules.media.domain.valueobject;

public record MediaType(String mimeType) {
    public MediaType {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Le type MIME ne peut pas être vide");
        }
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    public boolean isVideo() {
        return mimeType.startsWith("video/");
    }

    public boolean isDocument() {
        return mimeType.startsWith("application/") || 
               mimeType.equals("text/pdf") || 
               mimeType.equals("application/pdf");
    }

    public boolean isAllowed() {
        return isImage() || isVideo() || isDocument();
    }
}