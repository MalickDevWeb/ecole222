package ecole_221.sn.ecole.modules.media.application.port.usecase;

import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import java.io.IOException;

public interface UploadMediaUseCase {
    UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException;
    UploadedFile upload(byte[] fileBytes, String fileName, String contentType, String userId) throws IOException;
}
