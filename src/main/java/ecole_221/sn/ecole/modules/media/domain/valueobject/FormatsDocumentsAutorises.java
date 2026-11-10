package ecole_221.sn.ecole.modules.media.domain.valueobject;

import java.util.Locale;
import java.util.Set;

/**
 * Formats de documents acceptés pour les supports de cours et les rendus de devoirs (lot B, espace professeur) :
 * PDF, images et documents bureautiques (Word, Excel, PowerPoint, OpenDocument).
 *
 * <p>Le type déclaré par le navigateur ne suffit pas : la SIGNATURE réelle du contenu (octets magiques) est vérifiée.
 * Formats bureautiques modernes (docx, xlsx, pptx, odt, ods, odp) = archive ZIP « PK\3\4 » ;
 * anciens formats (doc, xls, ppt) = conteneur OLE « D0 CF 11 E0 A1 B1 1A E1 ».</p>
 */
public final class FormatsDocumentsAutorises {

    public static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024; // 10 Mo (limite multipart du serveur)

    public static final String LIBELLE = "PDF, images (PNG, JPG, WEBP, GIF) et documents bureautiques "
            + "(Word, Excel, PowerPoint, OpenDocument)";

    private static final Set<String> IMAGES_ET_PDF = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "application/pdf");

    private static final Set<String> BUREAUTIQUE_ZIP = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation");

    private static final Set<String> BUREAUTIQUE_OLE = Set.of(
            "application/msword", "application/vnd.ms-excel", "application/vnd.ms-powerpoint");

    private FormatsDocumentsAutorises() {}

    public static boolean estBureautique(String type) {
        String t = normaliser(type);
        return BUREAUTIQUE_ZIP.contains(t) || BUREAUTIQUE_OLE.contains(t);
    }

    public static boolean typeAutorise(String type) {
        String t = normaliser(type);
        return IMAGES_ET_PDF.contains(t) || BUREAUTIQUE_ZIP.contains(t) || BUREAUTIQUE_OLE.contains(t);
    }

    /** Signature des documents bureautiques (les images et le PDF sont vérifiés par UploadController). */
    public static boolean signatureBureautiqueConforme(byte[] b, String type) {
        if (b == null || b.length < 8) return false;
        String t = normaliser(type);
        if (BUREAUTIQUE_ZIP.contains(t)) {
            return (b[0] & 0xFF) == 0x50 && (b[1] & 0xFF) == 0x4B && (b[2] & 0xFF) == 0x03 && (b[3] & 0xFF) == 0x04;
        }
        if (BUREAUTIQUE_OLE.contains(t)) {
            int[] ole = {0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1};
            for (int i = 0; i < ole.length; i++) {
                if ((b[i] & 0xFF) != ole[i]) return false;
            }
            return true;
        }
        return false;
    }

    /** Signature réelle du contenu pour TOUS les formats autorisés. */
    public static boolean signatureConforme(byte[] b, String type) {
        String t = normaliser(type);
        if (estBureautique(t)) return signatureBureautiqueConforme(b, t);
        if (b == null || b.length < 12) return false;
        return switch (t) {
            case "application/pdf" -> commencePar(b, 0x25, 0x50, 0x44, 0x46);
            case "image/png" -> commencePar(b, 0x89, 0x50, 0x4E, 0x47);
            case "image/jpeg", "image/jpg" -> commencePar(b, 0xFF, 0xD8, 0xFF);
            case "image/gif" -> commencePar(b, 0x47, 0x49, 0x46, 0x38);
            case "image/webp" -> commencePar(b, 0x52, 0x49, 0x46, 0x46)
                    && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50;
            default -> false;
        };
    }

    private static boolean commencePar(byte[] b, int... octets) {
        for (int i = 0; i < octets.length; i++) {
            if ((b[i] & 0xFF) != octets[i]) return false;
        }
        return true;
    }

    private static String normaliser(String type) {
        return type == null ? "" : type.toLowerCase(Locale.ROOT).trim();
    }
}
