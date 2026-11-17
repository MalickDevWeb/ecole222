package ecole_221.sn.ecole.modules.archivage.application.format;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Écriture en flux du contenu d'une archive : JSON (Jackson) compressé GZIP, une section par table avec ses noms de
 * colonnes, ses lignes (tableaux de valeurs texte), puis son nombre de lignes et son empreinte SHA-256. Rien n'est
 * gardé en mémoire au-delà d'une ligne. Le flux de sortie reçu est déjà chiffrant (voir {@link ChiffrementFlux}).
 *
 * <pre>
 * {"format": "ecole221-archive-ecole", "versionFormat": 1, ...en-tête...,
 *  "tables": [{"nom": "...", "categorie": "...", "colonnes": [{"nom", "type"}], "cle": [...],
 *              "lignes": [[...], ...], "nombre": n, "empreinte": "..."}],
 *  "medias": [{"table", "colonne", "url"}], "resume": {...}}
 * </pre>
 */
public final class EcritureArchive implements Closeable {

    public static final String FORMAT = "ecole221-archive-ecole";
    public static final int VERSION_FORMAT = 1;
    private static final ObjectMapper JSON = new ObjectMapper();

    public record ResumeTable(long lignes, String empreinte) {
    }

    private final JsonGenerator json;
    private EmpreinteLignes empreinte;
    private boolean ferme;

    public EcritureArchive(OutputStream sortie) throws IOException {
        this.json = JSON.getFactory().createGenerator(new GZIPOutputStream(sortie, 64 * 1024), JsonEncoding.UTF8);
    }

    public void entete(Map<String, Object> entete) throws IOException {
        json.writeStartObject();
        json.writeStringField("format", FORMAT);
        json.writeNumberField("versionFormat", VERSION_FORMAT);
        for (Map.Entry<String, Object> e : entete.entrySet()) {
            json.writeObjectField(e.getKey(), e.getValue());
        }
        json.writeArrayFieldStart("tables");
    }

    public void debutTable(TableArchivee table) throws IOException {
        json.writeStartObject();
        json.writeStringField("nom", table.nom());
        json.writeStringField("categorie", table.categorie());
        json.writeArrayFieldStart("colonnes");
        for (TableArchivee.Colonne c : table.colonnes()) {
            json.writeStartObject();
            json.writeStringField("nom", c.nom());
            json.writeStringField("type", c.type());
            json.writeEndObject();
        }
        json.writeEndArray();
        json.writeArrayFieldStart("cle");
        for (String k : table.cle()) {
            json.writeString(k);
        }
        json.writeEndArray();
        json.writeArrayFieldStart("lignes");
        empreinte = new EmpreinteLignes();
    }

    public void ligne(String[] valeurs) throws IOException {
        json.writeStartArray();
        for (String v : valeurs) {
            if (v == null) {
                json.writeNull();
            } else {
                json.writeString(v);
            }
        }
        json.writeEndArray();
        empreinte.ajouter(valeurs);
    }

    public ResumeTable finTable() throws IOException {
        json.writeEndArray();
        String hex = empreinte.hex();
        json.writeNumberField("nombre", empreinte.lignes());
        json.writeStringField("empreinte", hex);
        json.writeEndObject();
        ResumeTable r = new ResumeTable(empreinte.lignes(), hex);
        empreinte = null;
        return r;
    }

    public void fin(Collection<Map<String, String>> medias, Map<String, Object> resume) throws IOException {
        json.writeEndArray();
        json.writeObjectField("medias", medias);
        json.writeObjectField("resume", resume);
        json.writeEndObject();
    }

    @Override
    public void close() throws IOException {
        if (!ferme) {
            ferme = true;
            json.close();   // ferme aussi le GZIP et le flux chiffrant (dernier segment)
        }
    }
}
