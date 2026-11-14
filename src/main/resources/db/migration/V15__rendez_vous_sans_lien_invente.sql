-- 2026-11-14 : les demandes de rendez-vous des parents étaient enregistrées « CONFIRME » d'office avec un lien Google
-- Meet inventé (https://meet.google.com/rdv-xxxxxxxx : aucune réunion n'existait, personne n'avait confirmé).
-- On remet ces demandes en attente et on retire les liens inventés ; les liens saisis par une personne sont gardés.
UPDATE parent_appointments
   SET meet_url = NULL,
       status = 'EN_ATTENTE'
 WHERE meet_url LIKE 'https://meet.google.com/rdv-%';
