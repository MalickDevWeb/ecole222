package ecole_221.sn.ecole.modules.archivage.application.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Lecture en flux d'une archive écrite par {@link EcritureArchive}. Pour chaque table, le nombre de lignes et
 * l'empreinte SHA-256 sont RECALCULÉS pendant la lecture et comparés à ceux rangés par l'export : le moindre écart
 * arrête la lecture (ArchivageException), avant toute suppression ou toute écriture.
 */
public final class LectureArchive {

    private static final ObjectMapper JSON = new ObjectMapper();

    private LectureArchive() {
    }

    public interface Visiteur {
        default void entete(Map<String, Object> entete) throws IOException {
        }

        default void debutTable(TableArchivee table) throws IOException {
        }

        void ligne(TableArchivee table, String[] valeurs) throws IOException;

        default void finTable(TableArchivee table, long lignes, String empreinte) throws IOException {
        }

        default void medias(List<Map<String, Object>> medias) throws IOException {
        }

        default void resume(Map<String, Object> resume) throws IOException {
        }
    }

    public static void lire(InputStream clair, Visiteur visiteur) throws IOException {
        try (JsonParser p = JSON.getFactory().createParser(new GZIPInputStream(clair, 64 * 1024))) {
            attendre(p.nextToken(), JsonToken.START_OBJECT);
            Map<String, Object> entete = new LinkedHashMap<>();
            boolean tablesLues = false;
            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String champ = p.currentName();
                p.nextToken();
                switch (champ) {
                    case "tables" -> {
                        verifierFormat(entete);
                        visiteur.entete(entete);
                        lireTables(p, visiteur);
                        tablesLues = true;
                    }
                    case "medias" -> visiteur.medias(lireListe(p));
                    case "resume" -> visiteur.resume(lireObjet(p));
                    default -> entete.put(champ, JSON.readValue(p, Object.class));
                }
            }
            if (!tablesLues) {
                throw ArchivageException.refus("Archive incomplète : aucune table.");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException | java.io.EOFException | java.util.zip.ZipException e) {
            throw ArchivageException.refus("Archive illisible : contenu tronqué ou mal formé.");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lireListe(JsonParser p) throws IOException {
        return JSON.readValue(p, List.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> lireObjet(JsonParser p) throws IOException {
        return JSON.readValue(p, Map.class);
    }

    private static void verifierFormat(Map<String, Object> entete) {
        if (!EcritureArchive.FORMAT.equals(entete.get("format"))
                || !(entete.get("versionFormat") instanceof Number n) || n.intValue() != EcritureArchive.VERSION_FORMAT) {
            throw ArchivageException.refus("Archive illisible : format inconnu.");
        }
    }

    private static void lireTables(JsonParser p, Visiteur visiteur) throws IOException {
        attendre(p.currentToken(), JsonToken.START_ARRAY);
        while (p.nextToken() == JsonToken.START_OBJECT) {
            lireTable(p, visiteur);
        }
        attendre(p.currentToken(), JsonToken.END_ARRAY);
    }

    private static void lireTable(JsonParser p, Visiteur visiteur) throws IOException {
        String nom = null;
        String categorie = null;
        List<TableArchivee.Colonne> colonnes = null;
        List<String> cle = new ArrayList<>();
        TableArchivee table = null;
        EmpreinteLignes calcul = new EmpreinteLignes();
        Long nombre = null;
        String empreinte = null;
        while (p.nextToken() == JsonToken.FIELD_NAME) {
            String champ = p.currentName();
            p.nextToken();
            switch (champ) {
                case "nom" -> nom = p.getText();
                case "categorie" -> categorie = p.getText();
                case "colonnes" -> {
                    colonnes = new ArrayList<>();
                    for (Map<String, Object> c : lireListe(p)) {
                        colonnes.add(new TableArchivee.Colonne(String.valueOf(c.get("nom")), String.valueOf(c.get("type"))));
                    }
                }
                case "cle" -> {
                    for (Object k : JSON.readValue(p, List.class)) {
                        cle.add(String.valueOf(k));
                    }
                }
                case "lignes" -> {
                    if (nom == null || colonnes == null) {
                        throw ArchivageException.refus("Archive illisible : table sans description.");
                    }
                    table = new TableArchivee(nom, categorie, List.copyOf(colonnes), List.copyOf(cle));
                    visiteur.debutTable(table);
                    attendre(p.currentToken(), JsonToken.START_ARRAY);
                    int largeur = colonnes.size();
                    while (p.nextToken() == JsonToken.START_ARRAY) {
                        String[] valeurs = new String[largeur];
                        int i = 0;
                        JsonToken t;
                        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
                            if (i >= largeur) {
                                throw ArchivageException.refus("Archive incohérente : ligne trop longue dans " + nom + ".");
                            }
                            if (t == JsonToken.VALUE_NULL) {
                                valeurs[i++] = null;
                            } else if (t == JsonToken.VALUE_STRING) {
                                valeurs[i++] = p.getText();
                            } else {
                                throw ArchivageException.refus("Archive incohérente : valeur inattendue dans " + nom + ".");
                            }
                        }
                        if (i != largeur) {
                            throw ArchivageException.refus("Archive incohérente : ligne incomplète dans " + nom + ".");
                        }
                        calcul.ajouter(valeurs);
                        visiteur.ligne(table, valeurs);
                    }
                }
                case "nombre" -> nombre = p.getLongValue();
                case "empreinte" -> empreinte = p.getText();
                default -> p.skipChildren();
            }
        }
        if (table == null || nombre == null || empreinte == null) {
            throw ArchivageException.refus("Archive incomplète : table " + (nom == null ? "?" : nom) + ".");
        }
        String recalculee = calcul.hex();
        if (calcul.lignes() != nombre || !recalculee.equals(empreinte)) {
            throw ArchivageException.refus("Archive incohérente : la table " + nom + " ne correspond pas à son empreinte.");
        }
        visiteur.finTable(table, nombre, empreinte);
    }

    private static void attendre(JsonToken lu, JsonToken attendu) {
        if (lu != attendu) {
            throw ArchivageException.refus("Archive illisible : structure inattendue.");
        }
    }
}
