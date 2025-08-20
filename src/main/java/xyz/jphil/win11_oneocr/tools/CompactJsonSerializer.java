package xyz.jphil.win11_oneocr.tools;

import xyz.jphil.win11_oneocr.*;
import org.json.*;
import java.util.*;

/**
 * Creates ultra-compact JSON output for OCR results using org.json
 Maintains ultra-compact bounds format while using proper JSON library
 */
public class CompactJsonSerializer {
    
    /**
     * Serialize OCR results to ultra-compact bounds-based JSON format
     * @param result OCR results
     * @param imageFile Source image path for metadata
     * @param imageWidth Image width in pixels
     * @param imageHeight Image height in pixels
     * @return Compact single-line JSON string
     */
    public static String toCompactJson(OcrResult result, String imageFile, int imageWidth, int imageHeight) {
        var root = new JSONObject();
        
        // Create metadata record
        var wordCount = result.lines().stream().mapToInt(l -> l.words().size()).sum();
        var metadata = OcrMetadata.create(imageFile, imageWidth, imageHeight, result.lines().size(), wordCount);
        
        // Metadata section (named properties)
        var meta = new JSONObject()
        .put("file", metadata.file())
        .put("size", new JSONArray().put(metadata.size()[0]).put(metadata.size()[1]))
        .put("timestampUTCISO", metadata.timestampUTCISO())
        .put("lines", metadata.lines())
        .put("words", metadata.words());
        root.put("meta", meta);
        
        // Document-level text angle
        root.put("angle", formatNumber(result.textAngle()));
        
        // Ultra-compact lines bounds: each line is [bounds, [words...]]
        var linesArray = new JSONArray();
        for (var line : result.lines()) {
            var lineArray = new JSONArray();
            
            // Line bounding box (null if not available)
            if (line.boundingBox() != null) {
                lineArray.put(createBoundsArray(line.boundingBox()));
            } else {
                lineArray.put(JSONObject.NULL);
            }
            
            // Words bounds: [[text,conf,bounds],...]
            var wordsArray = new JSONArray();
            for (var word : line.words()) {
                var wordArray = new JSONArray();
                wordArray.put(word.text());
                wordArray.put(formatConfidence(word.confidence()));
                
                if (word.boundingBox() != null) {
                    wordArray.put(createBoundsArray(word.boundingBox()));
                } else {
                    wordArray.put(JSONObject.NULL);
                }
                
                wordsArray.put(wordArray);
            }
            
            lineArray.put(wordsArray);
            linesArray.put(lineArray);
        }
        
        root.put("lines", linesArray);
        return root.toString();
    }
    
    private static JSONArray createBoundsArray(BoundingBox bbox) {
        var bounds = new JSONArray();
        var bnds = bbox.bounds();
        for (int i = 0; i < bnds.length; i++) {
            bounds.put(formatNumber(bnds[i]));   
        }
        return bounds;
    }
    
    /**
     * Smart number formatting: integer if no decimal part, double if has decimals
     */
    private static Number formatNumber(double value) {
        if (value == Math.floor(value)) {
            return (int) value; // Integer format
        } else {
            return Double.parseDouble(String.format("%.1f", value)); // Keep 1 decimal
        }
    }
    
    /**
     * Smart confidence formatting: 3 decimals max, remove trailing zeros
     */
    private static Number formatConfidence(double confidence) {
        var formatted = String.format("%.3f", confidence);
        // Remove trailing zeros but keep at least one decimal place
        formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", ".0");
        return Double.parseDouble(formatted);
    }
    
    /**
     * Deserialize JSON string back to OcrResult record
     */
    public static OcrJsonFile fromJson(String jsonString) {
        var root = new JSONObject(jsonString);
        
        // Parse metadata into OcrMetadata record
        var metaJson = root.getJSONObject("meta");
        var sizeArray = metaJson.getJSONArray("size");
        var metadata = new OcrMetadata(
            metaJson.getString("file"),
            new int[]{sizeArray.getInt(0), sizeArray.getInt(1)},
            metaJson.getString("timestampUTCISO"),
            metaJson.getInt("lines"),
            metaJson.getInt("words")
        );
        
        // Parse angle
        var angle = root.getDouble("angle");
        
        // Parse lines bounds
        var linesArray = root.getJSONArray("lines");
        var lines = new ArrayList<OcrLine>();
        
        for (int i = 0; i < linesArray.length(); i++) {
            var lineArray = linesArray.getJSONArray(i);
            
            // Parse line bounds (first element, can be null)
            BoundingBox lineBounds = null;
            if (!lineArray.isNull(0)) {
                var bnds = lineArray.getJSONArray(0);
                lineBounds = new BoundingBox(
                    bnds.getDouble(0), bnds.getDouble(1),
                    bnds.getDouble(2), bnds.getDouble(3),
                    bnds.getDouble(4), bnds.getDouble(5),
                    bnds.getDouble(6), bnds.getDouble(7)
                );
            }
            
            // Parse words bounds (second element)
            var wordsArray = lineArray.getJSONArray(1);
            var words = new ArrayList<OcrWord>();
            
            for (int j = 0; j < wordsArray.length(); j++) {
                var wordArray = wordsArray.getJSONArray(j);
                
                var text = wordArray.getString(0);
                var confidence = wordArray.getDouble(1);
                
                // Parse word bounds (third element, can be null)
                BoundingBox wordBounds = null;
                if (!wordArray.isNull(2)) {
                    var bnds = wordArray.getJSONArray(2);
                    wordBounds = new BoundingBox(
                        bnds.getDouble(0), bnds.getDouble(1),
                        bnds.getDouble(2), bnds.getDouble(3),
                        bnds.getDouble(4), bnds.getDouble(5),
                        bnds.getDouble(6), bnds.getDouble(7)
                    );
                }
                
                words.add(new OcrWord(text, wordBounds, confidence));
            }
            
            // Reconstruct line text from words
            var lineText = words.stream()
                .map(OcrWord::text)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b);
            
            lines.add(new OcrLine(lineText, lineBounds, words));
        }
        
        // Reconstruct full text from all lines
        var fullText = lines.stream()
            .map(OcrLine::text)
            .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
        
        return new OcrJsonFile(metadata, 
                new OcrResult(fullText, angle, lines));
    }

}