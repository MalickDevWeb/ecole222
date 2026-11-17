package ecole_221.sn.ecole.modules.archivage.application.format;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Empreinte SHA-256 des lignes d'une table, sur un encodage sans ambiguïté (valeur nulle : -1 ; sinon longueur puis
 * octets UTF-8). Calculée à l'export depuis la base, recalculée à la relecture de l'archive : les deux doivent
 * coïncider avant toute suppression.
 */
public final class EmpreinteLignes {

    private final MessageDigest md;
    private long lignes;

    public EmpreinteLignes() {
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public void ajouter(String[] valeurs) {
        for (String v : valeurs) {
            if (v == null) {
                entier(-1);
            } else {
                byte[] o = v.getBytes(StandardCharsets.UTF_8);
                entier(o.length);
                md.update(o);
            }
        }
        lignes++;
    }

    private void entier(int n) {
        md.update((byte) (n >>> 24));
        md.update((byte) (n >>> 16));
        md.update((byte) (n >>> 8));
        md.update((byte) n);
    }

    public long lignes() {
        return lignes;
    }

    public String hex() {
        return HexFormat.of().formatHex(md.digest());
    }
}
