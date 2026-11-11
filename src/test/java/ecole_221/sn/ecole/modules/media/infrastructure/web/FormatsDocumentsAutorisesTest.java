package ecole_221.sn.ecole.modules.media.infrastructure.web;

import ecole_221.sn.ecole.modules.media.domain.valueobject.FormatsDocumentsAutorises;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lot B : documents bureautiques acceptés, mais seulement si la signature du contenu correspond. */
class FormatsDocumentsAutorisesTest {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Test
    void bureautiqueModerneEtAncien() {
        byte[] zip = {0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] ole = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0, 0, 0, 0};
        assertTrue(FormatsDocumentsAutorises.typeAutorise(DOCX));
        assertTrue(FormatsDocumentsAutorises.signatureConforme(zip, DOCX));
        assertTrue(FormatsDocumentsAutorises.signatureConforme(ole, "application/msword"));
        assertFalse(FormatsDocumentsAutorises.signatureConforme(ole, DOCX));
        assertFalse(FormatsDocumentsAutorises.signatureConforme("<html></html>xx".getBytes(), DOCX));
    }

    @Test
    void imagesEtPdfInchanges_typesInconnusRefuses() {
        assertTrue(FormatsDocumentsAutorises.signatureConforme("%PDF-1.4 contenu".getBytes(), "application/pdf"));
        assertFalse(FormatsDocumentsAutorises.typeAutorise("application/x-msdownload"));
        assertFalse(FormatsDocumentsAutorises.typeAutorise(null));
        assertTrue(UploadController.signatureConforme("%PDF-1.4 contenu".getBytes(), "application/pdf"));
    }
}
