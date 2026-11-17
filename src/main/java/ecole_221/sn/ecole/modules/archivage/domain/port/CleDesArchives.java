package ecole_221.sn.ecole.modules.archivage.domain.port;

/**
 * Port : clé maîtresse des archives (variable d'environnement ARCHIVES_CLE, 32 octets en base64). Elle n'enveloppe que
 * les clés de données des archives ; elle n'est jamais rangée avec les archives, ni en base, ni dans le journal.
 */
public interface CleDesArchives {

    /** Clé maîtresse et son identifiant public (empreinte courte, ne révèle rien de la clé). */
    record CleMaitresse(byte[] octets, String identifiant) {
    }

    /** Refuse (ArchivageException INDISPONIBLE) si la clé manque ou est invalide là où elle est exigée. */
    CleMaitresse obtenir();
}
