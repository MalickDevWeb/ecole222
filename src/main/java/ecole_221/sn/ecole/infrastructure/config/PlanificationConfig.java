package ecole_221.sn.ecole.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tâches planifiées (2026-11-14) : purge de conservation du journal d'audit (PurgeJournalAudit). */
@Configuration
@EnableScheduling
public class PlanificationConfig {
}
