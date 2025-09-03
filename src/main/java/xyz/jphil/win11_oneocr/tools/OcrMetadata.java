package xyz.jphil.win11_oneocr.tools;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import xyz.jphil.win11_oneocr.OcrResult;

/**
 * Metadata information about the OCR processing
 */
public record OcrMetadata(
    String file,
    int width, int height,
    String timestampUTCISO,
    String plainText,
    OcrMetrics metrics
) {
    
    /**
     * Create metadata from basic information with current UTC timestamp
     */
    public static OcrMetadata create(String imageFile, int imageWidth, int imageHeight, OcrResult or) {
        var utcTimestamp = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT);
            
        
        return new OcrMetadata(
            imageFile,
            imageWidth, imageHeight,
            utcTimestamp,
            or.text(),
            new OcrMetrics(or)
        );
    }
}
