package xyz.jphil.win11_oneocr.tools;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Metadata information about the OCR processing
 */
public record OcrMetadata(
    String file,
    int[] size,
    String timestampUTCISO,
    int lines,
    int words
) {
    
    /**
     * Create metadata from basic information with current UTC timestamp
     */
    public static OcrMetadata create(String imageFile, int imageWidth, int imageHeight, int lineCount, int wordCount) {
        var utcTimestamp = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT);
            
        return new OcrMetadata(
            imageFile,
            new int[]{imageWidth, imageHeight},
            utcTimestamp,
            lineCount,
            wordCount
        );
    }
}
