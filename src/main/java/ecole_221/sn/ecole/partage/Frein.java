package ecole_221.sn.ecole.partage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frein à fenêtre glissante (15/11) : au plus {@code max} événements par clé pendant {@code fenetre}. Mémoire du
 * serveur (chaque instance compte de son côté) ; bornée à 20 000 clés (les fenêtres expirées sont purgées).
 * Verrouillage TEMPORAIRE seulement : la clé se libère d'elle-même à la fin de la fenêtre.
 */
public final class Frein {

    private final int max;
    private final Duration fenetre;
    private final Map<String, Deque<Instant>> evenements = new ConcurrentHashMap<>();
    public Clock horloge = Clock.systemUTC();

    public Frein(int max, Duration fenetre) {
        this.max = max;
        this.fenetre = fenetre;
    }

    /** Vrai si la clé a atteint la limite (sans rien compter). */
    public boolean bloque(String cle) {
        Deque<Instant> d = evenements.get(cle == null ? "?" : cle);
        if (d == null) {
            return false;
        }
        synchronized (d) {
            purger(d, horloge.instant());
            return d.size() >= max;
        }
    }

    /** Compte un événement. */
    public void noter(String cle) {
        Instant t = horloge.instant();
        if (evenements.size() > 20_000) {
            evenements.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    purger(e.getValue(), t);
                    return e.getValue().isEmpty();
                }
            });
        }
        Deque<Instant> d = evenements.computeIfAbsent(cle == null ? "?" : cle, k -> new ArrayDeque<>());
        synchronized (d) {
            purger(d, t);
            d.addLast(t);
        }
    }

    /** Compte l'événement s'il reste de la place ; faux (rien compté) si la limite est atteinte. */
    public boolean autoriser(String cle) {
        Instant t = horloge.instant();
        Deque<Instant> d = evenements.computeIfAbsent(cle == null ? "?" : cle, k -> new ArrayDeque<>());
        synchronized (d) {
            purger(d, t);
            if (d.size() >= max) {
                return false;
            }
            d.addLast(t);
            return true;
        }
    }

    public int compte(String cle) {
        Deque<Instant> d = evenements.get(cle == null ? "?" : cle);
        if (d == null) return 0;
        synchronized (d) {
            purger(d, horloge.instant());
            return d.size();
        }
    }

    private void purger(Deque<Instant> d, Instant t) {
        while (!d.isEmpty() && !d.peekFirst().isAfter(t.minus(fenetre))) {
            d.pollFirst();
        }
    }
}
