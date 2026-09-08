package ecole_221.sn.ecole.modules.media.domain.port;

import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import java.io.IOException;

public interface MediaStoragePort {
    UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException;
}
