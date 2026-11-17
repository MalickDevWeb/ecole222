package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Politique de mot de passe UNIQUE (15/11, conception DevSecOps de Fallou Ndiaye §4) : création d'un compte,
 * activation par lien, réinitialisation, changement par le titulaire, inscription publique d'une école.
 *
 * <ul>
 *   <li>longueur minimale selon le rôle : 10 (élèves, parents), 12 (personnel et administration d'école),
 *       14 (super-administrateur, administrateur de la plateforme) ; 128 au plus ;</li>
 *   <li>phrases de passe acceptées (espaces, accents) ; texte normalisé NFKC ; plus de « une lettre et un chiffre » ;</li>
 *   <li>refusés : mots de passe courants (liste embarquée {@code securite/mots-de-passe-courants.txt}), mots de passe
 *       contenant le prénom, le nom, le matricule, la partie locale de l'e-mail, le téléphone, le nom ou l'identifiant
 *       de l'école ; répétitions et suites (aaaaaaaaaa, 1234567890) ; l'ancien mot de passe.</li>
 * </ul>
 * Chaque refus dit quoi faire, en français.
 */
public final class PolitiqueMotDePasse {

    private static final Logger log = LoggerFactory.getLogger(PolitiqueMotDePasse.class);

    public static final int LONGUEUR_MAX = 128;
    public static final int MIN_ELEVE_PARENT = 10;
    public static final int MIN_ECOLE = 12;
    public static final int MIN_PLATEFORME = 14;
    static final String RESSOURCE = "/securite/mots-de-passe-courants.txt";
    private static final Set<String> ROLES_10 = Set.of("ETUDIANT", "STUDENT", "PARENT");
    private static final Set<String> ROLES_14 = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * Mot interdit et ce qu'il représente (« votre prénom », « le nom de l'école »…), pour dire précisément pourquoi
     * le mot de passe est refusé (16/11 : avant, un seul message citait tous les cas, suivi d'une astuce qui répétait
     * les conseils affichés sous le champ — « trop de mots » selon le Tech Lead).
     */
    public record MotInterdit(String valeur, String libelle) {
    }

    private PolitiqueMotDePasse() {
    }

    /** Liste chargée une seule fois (≈ 21 000 entrées). */
    private static final class Liste {
        static final Set<String> COURANTS = charger();

        private static Set<String> charger() {
            Set<String> s = new HashSet<>(32_768);
            try (InputStream in = PolitiqueMotDePasse.class.getResourceAsStream(RESSOURCE)) {
                if (in == null) {
                    log.error("Liste des mots de passe courants introuvable ({}) : seule la longueur est contrôlée", RESSOURCE);
                    return s;
                }
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        l = l.trim();
                        if (!l.isEmpty() && !l.startsWith("#")) {
                            s.add(comparable(l));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Lecture de la liste des mots de passe courants impossible : {}", e.toString());
            }
            return s;
        }
    }

    static int nombreCourants() {
        return Liste.COURANTS.size();
    }

    /** Longueur minimale pour un rôle (rôle inconnu ou absent : règle de l'administration d'école). */
    public static int longueurMin(String role) {
        String r = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (ROLES_10.contains(r)) return MIN_ELEVE_PARENT;
        if (ROLES_14.contains(r)) return MIN_PLATEFORME;
        return MIN_ECOLE;
    }

    /** Règles exposées au frontend (indicateur de force) : GET /auth/regles-mot-de-passe?role=. */
    public static Map<String, Object> regles(String role) {
        String r = role == null || role.isBlank() ? "ADMIN" : role.trim().toUpperCase(Locale.ROOT);
        return Map.of(
                "role", r,
                "longueurMin", longueurMin(r),
                "longueurMax", LONGUEUR_MAX,
                // 16/11 : conseils raccourcis (le premier, sur la longueur, est filtré côté écran par « Au moins N caractères »)
                "conseils", List.of(
                        "Au moins " + longueurMin(r) + " caractères ; espaces et accents acceptés.",
                        "Une phrase de plusieurs mots est plus sûre.",
                        "Ni votre nom, ni votre prénom, ni votre téléphone, ni le nom de l'école.",
                        "Pas de mot de passe courant (passer123…) ni de suite (123456…).",
                        "Ne le réutilisez nulle part ailleurs."));
    }

    /**
     * Message d'erreur, ou null si le mot de passe est accepté.
     *
     * @param compte          compte concerné (null à l'inscription publique d'une école : règles de l'administration)
     * @param motsPersonnels  autres mots interdits, considérés comme le nom ou l'identifiant de l'école
     */
    public static String erreur(String motDePasse, UserJpaEntity compte, Collection<String> motsPersonnels) {
        List<MotInterdit> mots = new ArrayList<>();
        if (motsPersonnels != null) {
            for (String m : motsPersonnels) mots.add(new MotInterdit(m, "le nom de l'école"));
        }
        return erreurDetaillee(motDePasse, compte, mots);
    }

    /**
     * Même règle, chaque mot interdit portant ce qu'il représente : le refus cite précisément le problème
     * (« Le mot de passe contient votre prénom et votre nom : choisissez-en un autre. »).
     */
    public static String erreurDetaillee(String motDePasse, UserJpaEntity compte, List<MotInterdit> motsInterdits) {
        if (motDePasse == null || motDePasse.isBlank()) {
            return "Le mot de passe est obligatoire.";
        }
        String n = Normalizer.normalize(motDePasse, Normalizer.Form.NFKC);
        int longueur = n.codePointCount(0, n.length());
        if (longueur > LONGUEUR_MAX) {
            return "Le mot de passe ne doit pas dépasser " + LONGUEUR_MAX + " caractères.";
        }
        int min = longueurMin(compte == null ? "ADMIN" : compte.getRole());
        if (longueur < min) {
            return "Le mot de passe doit contenir au moins " + min + " caractères.";
        }
        String c = comparable(n);
        if (Liste.COURANTS.contains(c) || Liste.COURANTS.contains(sansChiffresFinaux(c))) {
            return "Ce mot de passe est trop courant : choisissez-en un autre.";
        }
        if (repetitionOuSuite(c)) {
            return "Le mot de passe ne doit pas être une répétition ou une suite (aaaa, 1234…).";
        }
        // mots de 4 lettres et plus : interdits partout ; mots de 3 lettres (« Awa ») : seulement comme mot entier,
        // pour ne pas refuser une phrase de passe où ces lettres se suivent par hasard
        Set<String> motsSaisis = new HashSet<>();
        for (String morceau : n.split("[\\s\\p{Punct}]+")) {
            motsSaisis.add(comparable(morceau));
        }
        List<String> trouves = new ArrayList<>();
        List<String> valeursTrouvees = new ArrayList<>();
        for (MotInterdit mot : motsPersonnels(compte, motsInterdits)) {
            String v = mot.valeur();
            if (!(v.length() >= 4 ? c.contains(v) : motsSaisis.contains(v))) continue;
            // déjà expliqué par un cas précédent (« awa » prénom ET début de l'e-mail « awa@… ») : on ne le cite qu'une fois
            boolean dejaExplique = valeursTrouvees.stream().anyMatch(t -> t.contains(v) || v.contains(t));
            valeursTrouvees.add(v);
            if (!dejaExplique && !trouves.contains(mot.libelle())) {
                trouves.add(mot.libelle());
            }
        }
        if (!trouves.isEmpty()) {
            return "Le mot de passe contient " + enumeration(trouves) + " : choisissez-en un autre.";
        }
        if (compte != null && compte.getPassword() != null && compte.getPassword().startsWith("$2")) {
            try {
                if (BCRYPT.matches(motDePasse, compte.getPassword())) {
                    return "Le nouveau mot de passe doit être différent de l'ancien.";
                }
            } catch (IllegalArgumentException e) {
                // empreinte illisible : rien à comparer
            }
        }
        return null;
    }

    /** Minuscules, sans accent, sans espace ni ponctuation : « Sénégal 2024 ! » → « senegal2024 ». */
    static String comparable(String s) {
        String n = Normalizer.normalize(Normalizer.normalize(s, Normalizer.Form.NFKC), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(n.length());
        n.codePoints().filter(Character::isLetterOrDigit).forEach(b::appendCodePoint);
        return b.toString();
    }

    private static String sansChiffresFinaux(String c) {
        return c.replaceAll("\\d+$", "");
    }

    /** Un seul caractère répété, deux caractères alternés, ou une suite croissante / décroissante (1234567890, abcdef…). */
    static boolean repetitionOuSuite(String c) {
        if (c.length() < 4) {
            return false;
        }
        if (c.chars().distinct().count() <= 2) {
            return true;
        }
        boolean monte = true;
        boolean descend = true;
        for (int i = 1; i < c.length(); i++) {
            int d = c.charAt(i) - c.charAt(i - 1);
            // 0 après 9 dans « 1234567890 » : suite des touches du clavier
            boolean suivant = d == 1 || (c.charAt(i - 1) == '9' && c.charAt(i) == '0');
            boolean precedent = d == -1 || (c.charAt(i - 1) == '0' && c.charAt(i) == '9');
            monte &= suivant;
            descend &= precedent;
        }
        return monte || descend;
    }

    /** « a », « a et b », « a, b et c ». */
    private static String enumeration(List<String> elements) {
        if (elements.size() == 1) return elements.get(0);
        return String.join(", ", elements.subList(0, elements.size() - 1)) + " et " + elements.get(elements.size() - 1);
    }

    /** Téléphone comparé sur ses 9 derniers chiffres (« +221 77 800 80 80 » → 778008080). */
    public static String chiffresTelephone(String telephone) {
        if (telephone == null) return null;
        String chiffres = telephone.replaceAll("\\D", "");
        return chiffres.length() > 9 ? chiffres.substring(chiffres.length() - 9) : chiffres;
    }

    private static List<MotInterdit> motsPersonnels(UserJpaEntity compte, List<MotInterdit> autres) {
        List<MotInterdit> brut = new ArrayList<>();
        if (compte != null) {
            brut.add(new MotInterdit(compte.getPrenom(), "votre prénom"));
            brut.add(new MotInterdit(compte.getNom(), "votre nom"));
            brut.add(new MotInterdit(compte.getMatricule(), "votre matricule"));
            String e = compte.getEmail();
            if (e != null && e.contains("@")) {
                brut.add(new MotInterdit(e.substring(0, e.indexOf('@')), "votre adresse e-mail"));
            }
            brut.add(new MotInterdit(chiffresTelephone(compte.getTelephone()), "votre numéro de téléphone"));
        }
        if (autres != null) {
            brut.addAll(autres);
        }
        List<MotInterdit> mots = new ArrayList<>();
        for (MotInterdit b : brut) {
            if (b == null || b.valeur() == null) continue;
            // un nom composé compte mot par mot (« Ndeye Fatou » → ndeye, fatou) ; mots de moins de 3 lettres ignorés
            for (String morceau : b.valeur().split("[\\s\\-_.@]+")) {
                String m = comparable(morceau);
                if (m.length() >= 3) mots.add(new MotInterdit(m, b.libelle()));
            }
            String entier = comparable(b.valeur());
            if (entier.length() >= 3) mots.add(new MotInterdit(entier, b.libelle()));
        }
        return mots;
    }
}
