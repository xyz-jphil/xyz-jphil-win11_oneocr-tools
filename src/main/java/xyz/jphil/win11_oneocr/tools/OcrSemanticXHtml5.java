package xyz.jphil.win11_oneocr.tools;
public class OcrSemanticXHtml5 {
    public static final String 
            PUBROOT = "https://xyz-jphil.github.io/win11_oneocr_semantic_xhtml/",
            CSS     = PUBROOT+"styles.css",
            JS      = PUBROOT+"scripts.js",
            DEFINITION = "Browser-renderable, Win11-One-OCR format optimized for AI comprehension of text patterns and spelling corrections due to xml/HTML-like structure. Structure: `<section><segment><w>` where 'segment'=detected text segment (lines/cells/chunks), 'w'=word. Key attributes: 'b'=bounding box coordinates, 'p'=probability/confidence score, 'i'=word-index, 'num'=segment-number, 'angle'=page skew/text rotation.",
            TRIVIA = "Only ~10% larger than ultra-compact JSON but significantly more AI-parseable. Browser-compatible with external CSS/JS for rich rendering and dynamic features. Decoupled presentation layer allows rendering upgrades without document modification. Combines machine efficiency with human readability and AI semantic understanding.",
            TYPE="Win11-OneOcr Semantic XHTML5"
    ;
    
    
}
