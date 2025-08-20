package xyz.jphil.win11_oneocr.tools;

import xyz.jphil.win11_oneocr.OcrResult;

public record OcrJsonFile(OcrMetadata metadata, OcrResult data) {
    
}
