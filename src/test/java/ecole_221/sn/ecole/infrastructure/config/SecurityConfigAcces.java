package ecole_221.sn.ecole.infrastructure.config;

import java.util.List;

/** Accès de test aux listes de routes publiques (paquet de SecurityConfig). */
public final class SecurityConfigAcces {
    private SecurityConfigAcces() {}
    public static List<String> lecturePublique() { return List.of(SecurityConfig.PORTAIL_CANDIDAT_LECTURE); }
    public static List<String> ecriturePublique() { return List.of(SecurityConfig.PORTAIL_CANDIDAT_ECRITURE); }
}
