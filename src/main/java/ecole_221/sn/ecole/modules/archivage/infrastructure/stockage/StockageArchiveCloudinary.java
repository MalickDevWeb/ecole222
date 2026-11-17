package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Stockage des archives chez Cloudinary (production) : fichiers bruts en accès privé, DÉCOUPÉS en morceaux de moins de
 * 10 Mo (taille maximale d'un fichier brut dans l'offre gratuite), plus un manifeste qui liste les morceaux avec leur
 * taille et leur empreinte SHA-256. La relecture vérifie chaque morceau.
 *
 * <p>Organisation : {@code <dossier>/<archive>/partie-0001.bin …} et {@code <dossier>/<archive>/manifeste.json} ;
 * l'emplacement rangé avec l'archive est l'identifiant du manifeste.</p>
 */
public class StockageArchiveCloudinary implements StockageArchive {

    /** 9 Mio = 9 437 184 octets : sous la limite de 10 Mo des fichiers bruts (offre gratuite). */
    static final int MORCEAU = 9 * 1024 * 1024;
    static final String FORMAT_MANIFESTE = "ecole221-archive-morceaux";
    private static final Pattern NOM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,180}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ClientCloudinaryArchives client;
    private final String dossier;
    private final int morceau;

    public StockageArchiveCloudinary(ClientCloudinaryArchives client, String dossier) {
        this(client, dossier, MORCEAU);
    }

    StockageArchiveCloudinary(ClientCloudinaryArchives client, String dossier, int morceau) {
        this.client = client;
        this.dossier = dossier == null || dossier.isBlank() ? "ecole221-archives" : dossier.replaceAll("^/+|/+$", "");
        this.morceau = morceau;
    }

    @Override
    public String nom() {
        return "cloudinary";
    }

    @Override
    public void verifierPret() {
        if (client == null) {
            throw ArchivageException.indisponible("Archivage refusé : Cloudinary n'est pas configuré "
                    + "(CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET).");
        }
    }

    @Override
    public String ecrire(String nomArchive, Path fichier) throws IOException {
        verifierPret();
        if (!NOM.matcher(nomArchive).matches()) {
            throw ArchivageException.refus("Nom d'archive invalide.");
        }
        String base = dossier + "/" + nomArchive;
        List<Map<String, Object>> morceaux = new ArrayList<>();
        MessageDigest total = sha256();
        long taille = 0;
        try (InputStream in = Files.newInputStream(fichier)) {
            int numero = 1;
            byte[] lu;
            while ((lu = in.readNBytes(morceau)).length > 0) {
                String id = base + "/partie-" + String.format("%04d", numero) + ".bin";
                client.televerser(id, lu);
                total.update(lu);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("publicId", id);
                m.put("taille", lu.length);
                m.put("sha256", HexFormat.of().formatHex(sha256().digest(lu)));
                morceaux.add(m);
                taille += lu.length;
                numero++;
            }
        }
        Map<String, Object> manifeste = new LinkedHashMap<>();
        manifeste.put("format", FORMAT_MANIFESTE);
        manifeste.put("version", 1);
        manifeste.put("taille", taille);
        manifeste.put("sha256", HexFormat.of().formatHex(total.digest()));
        manifeste.put("morceaux", morceaux);
        String idManifeste = base + "/manifeste.json";
        client.televerser(idManifeste, JSON.writeValueAsBytes(manifeste));
        return idManifeste;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> morceaux(String emplacement) throws IOException {
        Map<String, Object> manifeste;
        try {
            manifeste = JSON.readValue(client.telecharger(emplacement), Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw ArchivageException.refus("Manifeste de l'archive illisible.");
        }
        if (!FORMAT_MANIFESTE.equals(manifeste.get("format")) || !(manifeste.get("morceaux") instanceof List<?>)) {
            throw ArchivageException.refus("Manifeste de l'archive illisible.");
        }
        return (List<Map<String, Object>>) manifeste.get("morceaux");
    }

    @Override
    public InputStream lire(String emplacement) throws IOException {
        verifierPret();
        List<Map<String, Object>> liste = morceaux(emplacement);
        return new InputStream() {
            private int suivant;
            private InputStream courant = InputStream.nullInputStream();

            private boolean avancer() throws IOException {
                if (suivant >= liste.size()) return false;
                Map<String, Object> m = liste.get(suivant++);
                byte[] octets = client.telecharger(String.valueOf(m.get("publicId")));
                String attendu = String.valueOf(m.get("sha256"));
                if (octets.length != ((Number) m.get("taille")).intValue()
                        || !HexFormat.of().formatHex(sha256().digest(octets)).equals(attendu)) {
                    throw ArchivageException.refus("Morceau " + suivant + " de l'archive altéré dans Cloudinary.");
                }
                courant = new ByteArrayInputStream(octets);
                return true;
            }

            @Override
            public int read() throws IOException {
                byte[] un = new byte[1];
                int n = read(un, 0, 1);
                return n < 0 ? -1 : un[0] & 0xff;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) return 0;
                while (true) {
                    int n = courant.read(b, off, len);
                    if (n >= 0) return n;
                    if (!avancer()) return -1;
                }
            }
        };
    }

    @Override
    public void supprimer(String emplacement) throws IOException {
        verifierPret();
        if (!client.existe(emplacement)) {
            return;
        }
        for (Map<String, Object> m : morceaux(emplacement)) {
            client.detruire(String.valueOf(m.get("publicId")));
        }
        client.detruire(emplacement);
    }

    @Override
    public boolean existe(String emplacement) throws IOException {
        verifierPret();
        return client.existe(emplacement);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
