package ecole_221.sn.ecole.modules.student.infrastructure.web;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de génération de relevés de notes PDF côté serveur.
 * Utilise OpenPDF (fork libre d'iText) pour produire un document officiel.
 */
@Service
public class GradesReportService {

    private static final Color RED_221     = new Color(179, 24, 28);
    private static final Color DARK_221    = new Color(41, 23, 21);
    private static final Color LIGHT_BG    = new Color(250, 249, 247);
    private static final Color BORDER_GRAY = new Color(226, 220, 218);
    private static final Color TEXT_GRAY   = new Color(100, 100, 100);

    public byte[] generateTranscriptPdf(String studentName, String parcours, String niveau,
                                         double average, double gpa, int ects,
                                         List<Map<String, Object>> modules) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, DARK_221);
            Font subtFont   = FontFactory.getFont(FontFactory.HELVETICA, 9f, TEXT_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, RED_221);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, TEXT_GRAY);
            Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, DARK_221);
            Font whiteFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
            Font smallGray  = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_GRAY);
            Font redSmall   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, RED_221);

            // ── EN-TÊTE ──────────────────────────────────────────────
            Paragraph header = new Paragraph();
            header.add(new Chunk("ECOLE 221 — PORTAIL ACADEMIQUE\n", titleFont));
            header.add(new Chunk("RELEVE DE NOTES OFFICIEL — SEMESTRE 2\n", subtFont));
            header.add(new Chunk("Annee 2023-2024 | " + niveau + " | " + parcours + "\n", subtFont));
            String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            header.add(new Chunk("Genere le " + genDate, smallGray));
            header.setSpacingAfter(14);
            document.add(header);

            // Ligne séparatrice
            PdfPTable sep = new PdfPTable(1);
            sep.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorderWidthTop(0);
            sepCell.setBorderWidthLeft(0);
            sepCell.setBorderWidthRight(0);
            sepCell.setBorderWidthBottom(1.5f);
            sepCell.setBorderColorBottom(RED_221);
            sepCell.setFixedHeight(2);
            sep.addCell(sepCell);
            sep.setSpacingAfter(12);
            document.add(sep);

            // ── INFORMATIONS ÉTUDIANT ─────────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(14);

            PdfPCell infoLeft = new PdfPCell();
            infoLeft.setBorder(com.lowagie.text.Rectangle.BOX);
            infoLeft.setBorderColor(BORDER_GRAY);
            infoLeft.setBackgroundColor(LIGHT_BG);
            infoLeft.setPadding(10);
            Paragraph leftP = new Paragraph();
            leftP.add(new Chunk("INFORMATIONS ETUDIANT\n\n", sectionFont));
            leftP.add(new Chunk("Nom : " + studentName + "\n", normalFont));
            leftP.add(new Chunk("Parcours : " + parcours + "\n", normalFont));
            leftP.add(new Chunk("Niveau : " + niveau, normalFont));
            infoLeft.addElement(leftP);
            infoTable.addCell(infoLeft);

            String decision = average >= 14 ? "Admis(e) avec mention" : average >= 10 ? "Admis sous reserve" : "Session de rattrapage";
            PdfPCell infoRight = new PdfPCell();
            infoRight.setBorder(com.lowagie.text.Rectangle.BOX);
            infoRight.setBorderColor(BORDER_GRAY);
            infoRight.setBackgroundColor(LIGHT_BG);
            infoRight.setPadding(10);
            Paragraph rightP = new Paragraph();
            rightP.add(new Chunk("DECISION PROVISOIRE\n\n", sectionFont));
            rightP.add(new Chunk(decision + "\n", normalFont));
            rightP.add(new Chunk("Annee academique : 2023-2024", normalFont));
            infoRight.addElement(rightP);
            infoTable.addCell(infoRight);
            document.add(infoTable);

            // ── SYNTHÈSE ────────────────────────────────────────────
            document.add(new Paragraph("SYNTHESE DU SEMESTRE 2\n", sectionFont));

            String mention = average >= 16 ? "Tres Bien" : average >= 14 ? "Bien" : average >= 12 ? "Assez Bien" : average >= 10 ? "Passable" : "Rattrapage";
            int ectsPercent = (int) Math.round((ects / 30.0) * 100);

            PdfPTable synthTable = new PdfPTable(4);
            synthTable.setWidthPercentage(100);
            synthTable.setSpacingBefore(6);
            synthTable.setSpacingAfter(14);
            String[][] synthData = {
                {"MOYENNE S2", String.format("%.2f/20", average), "Mention " + mention},
                {"GPA ESTIME", String.format("%.1f/4.0", gpa), average >= 14 ? "Excellent" : "Satisfaisant"},
                {"ECTS ACQUIS", ects + "/30", ectsPercent + "% valides"},
                {"PROMOTION", "En cours", "142 etudiants"}
            };
            for (String[] row : synthData) {
                PdfPCell c = new PdfPCell();
                c.setBorder(com.lowagie.text.Rectangle.BOX);
                c.setBorderColor(BORDER_GRAY);
                c.setPadding(8);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                Paragraph cp = new Paragraph();
                cp.setAlignment(Element.ALIGN_CENTER);
                cp.add(new Chunk(row[0] + "\n", smallGray));
                cp.add(new Chunk(row[1] + "\n", boldFont));
                cp.add(new Chunk(row[2], redSmall));
                c.addElement(cp);
                synthTable.addCell(c);
            }
            document.add(synthTable);

            // ── TABLEAU DES NOTES ─────────────────────────────────────
            document.add(new Paragraph("DETAIL DES MODULES\n", sectionFont));

            PdfPTable gradesTable = new PdfPTable(new float[]{40, 12, 12, 12, 12, 12});
            gradesTable.setWidthPercentage(100);
            gradesTable.setSpacingBefore(6);

            String[] headers = {"Module & Professeur", "ECTS", "CC", "Examen", "Note", "Statut"};
            for (String h : headers) {
                PdfPCell hc = new PdfPCell(new Phrase(h, whiteFont));
                hc.setBackgroundColor(DARK_221);
                hc.setPadding(7);
                hc.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                gradesTable.addCell(hc);
            }

            for (int i = 0; i < modules.size(); i++) {
                Map<String, Object> m = modules.get(i);
                String modName = String.valueOf(m.getOrDefault("module", "—"));
                String prof = String.valueOf(m.getOrDefault("prof", "—"));
                double cc = toDouble(m.getOrDefault("cc", 0));
                double examen = toDouble(m.getOrDefault("examen", 0));
                double note = (cc * 0.4) + (examen * 0.6);
                int ectsM = toInt(m.getOrDefault("ects", 0));
                boolean valide = note >= 10;
                Color rowBg = (i % 2 == 0) ? LIGHT_BG : Color.WHITE;

                // Module + Prof
                PdfPCell cMod = new PdfPCell();
                cMod.setBackgroundColor(rowBg);
                cMod.setPadding(6);
                cMod.setBorder(com.lowagie.text.Rectangle.BOTTOM);
                cMod.setBorderColor(BORDER_GRAY);
                Paragraph modP = new Paragraph();
                modP.add(new Chunk(modName + "\n", boldFont));
                modP.add(new Chunk(prof, smallGray));
                cMod.addElement(modP);
                gradesTable.addCell(cMod);

                addCell(gradesTable, ectsM + "", rowBg, normalFont);
                addCell(gradesTable, String.format("%.1f", cc), rowBg, normalFont);
                addCell(gradesTable, String.format("%.1f", examen), rowBg, normalFont);
                addCell(gradesTable, String.format("%.2f", note), rowBg,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, RED_221));
                Font statFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f,
                        valide ? new Color(16, 124, 65) : RED_221);
                addCell(gradesTable, valide ? "VALIDE" : "RATTRAP.", rowBg, statFont);
            }
            document.add(gradesTable);

            // ── SIGNATURE ─────────────────────────────────────────────
            String uid = "E221-S2-" + (10000 + Math.abs(UUID.randomUUID().hashCode()) % 89999);
            Paragraph sign = new Paragraph();
            sign.setSpacingBefore(24);
            sign.add(new Chunk("SIGNATURE & HOMOLOGATION\n\n", sectionFont));
            sign.add(new Chunk("Le Conseil de Direction Academique de l'Ecole 221\n", normalFont));
            sign.add(new Chunk("Releve de notes officiel provisoire — " + genDate + "\n", normalFont));
            sign.add(new Chunk("ID : " + uid + "\n\n", smallGray));
            sign.add(new Chunk("DIRECTION DES ETUDES — CERTIFIE", redSmall));
            document.add(sign);

            // ── PIED DE PAGE ──────────────────────────────────────────
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(30);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.add(new Chunk("Ecole 221 — Etablissement d'Enseignement Superieur Prive Agree par l'Etat\n", smallGray));
            footer.add(new Chunk("(c) 2026 Ecole 221. Tous droits reserves.", smallGray));
            document.add(footer);

        } catch (Exception e) {
            throw new RuntimeException("Erreur generation PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Color bg, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(bg);
        c.setPadding(6);
        c.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        c.setBorderColor(BORDER_GRAY);
        table.addCell(c);
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }
}
