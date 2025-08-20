package xyz.jphil.win11_oneocr.tools;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.jphil.win11_oneocr.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * Professional command-line OCR tool using Windows 11 OneOCR
 * Built with PicoCLI for robust argument parsing and help generation
 */
@Command(
    name = "oneocr", 
    mixinStandardHelpOptions = true, 
    version = "1.0",
    description = "Windows 11 OneOCR command-line tool - Extract text from images using Windows built-in OCR"
)
public class OcrTool implements Callable<Integer> {

    @Parameters(index = "0", description = "Input image file (JPG, PNG, BMP, TIFF)")
    private File inputFile;

    @Option(names = {"-o", "--output"}, description = "Output text file (default: stdout)")
    private File outputFile;

    @Option(names = {"--svg"}, description = "Generate SVG visualization (default: input.ext.oneocr.svg)")
    private File svgFile;

    @Option(names = {"--json"}, description = "Output compact JSON format (default: input.ext.oneocr.json)")
    private File jsonFile;

    @Option(names = {"-t", "--text"}, description = "Output plain text (default: input.ext.oneocr.txt)")
    private File textFile;

    @Option(names = {"--no-defaults"}, description = "Don't generate default outputs, only specified files")
    private boolean noDefaults;

    @Option(names = {"--max-lines"}, description = "Maximum number of text lines to recognize", defaultValue = "1000")
    private int maxLines;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"--show-confidence"}, description = "Show confidence scores for each word")
    private boolean showConfidence;

    @Option(names = {"--min-confidence"}, description = "Minimum confidence threshold (0.0-1.0)", defaultValue = "0.0")
    private double minConfidence;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OcrTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            if (verbose) {
                System.err.println("Loading image: " + inputFile.getAbsolutePath());
            }

            // Validate input file
            if (!inputFile.exists()) {
                System.err.println("Error: Input file does not exist: " + inputFile);
                return 1;
            }

            // Load and process image
            BufferedImage image = ImageIO.read(inputFile);
            if (image == null) {
                System.err.println("Error: Unable to read image file: " + inputFile);
                return 1;
            }

            byte[] bgraData = convertToBGRA(image);
            
            if (verbose) {
                System.err.printf("Image loaded: %dx%d pixels%n", image.getWidth(), image.getHeight());
                System.err.println("Initializing OCR engine...");
            }

            // Perform OCR
            OcrResult result;
            try (var ocrApi = new OneOcrApi()) {
                var initOptions = ocrApi.createInitOptions();
                var pipeline = ocrApi.createPipeline(initOptions);
                var processOptions = ocrApi.createProcessOptions(maxLines);

                if (verbose) {
                    System.err.println("Running OCR recognition...");
                }

                result = ocrApi.recognizeImage(pipeline, processOptions, 
                    image.getWidth(), image.getHeight(), bgraData);

                // Cleanup
                processOptions.close();
                pipeline.close();
                initOptions.close();
            }

            if (verbose) {
                System.err.printf("OCR completed: %d lines, %d words found%n", 
                    result.lines().size(), 
                    result.lines().stream().mapToInt(l -> l.words().size()).sum());
            }

            // Filter by confidence if specified
            if (minConfidence > 0.0) {
                result = filterByConfidence(result, minConfidence);
                if (verbose) {
                    System.err.printf("After confidence filtering (>%.2f): %d lines, %d words%n", 
                        minConfidence,
                        result.lines().size(), 
                        result.lines().stream().mapToInt(l -> l.words().size()).sum());
                }
            }

            // Determine output files (use defaults if not specified and not disabled)
            File actualTextFile = textFile;
            File actualJsonFile = jsonFile;  
            File actualSvgFile = svgFile;
            
            if (!noDefaults) {
                if (actualTextFile == null) {
                    actualTextFile = getDefaultOutputFile("txt");
                }
                if (actualJsonFile == null) {
                    actualJsonFile = getDefaultOutputFile("json");
                }
                if (actualSvgFile == null) {
                    actualSvgFile = getDefaultOutputFile("svg");
                }
            }

            // Generate outputs
            if (actualTextFile != null) {
                outputPlainText(result, actualTextFile);
            }
            if (actualJsonFile != null) {
                outputCompactJson(result, actualJsonFile, image.getWidth(), image.getHeight());
            }
            if (actualSvgFile != null) {
                generateSvg(result, actualSvgFile, image.getWidth(), image.getHeight());
                if (verbose) {
                    System.err.printf("SVG visualization saved to: %s%n", actualSvgFile.getName());
                }
            }
            
            // Fallback to stdout if no outputs specified
            if (actualTextFile == null && actualJsonFile == null && actualSvgFile == null) {
                outputStructuredText(result);
            }

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * Generate default output file name in pattern: input.ext.oneocr.{extension}
     */
    private File getDefaultOutputFile(String extension) {
        String inputFileName = inputFile.getName();
        String defaultName = inputFileName + ".oneocr." + extension;
        return new File(inputFile.getParent(), defaultName);
    }

    private void outputPlainText(OcrResult result, File textFile) throws Exception {
        String text = result.text();
        Files.writeString(textFile.toPath(), text);
        
        if (verbose) {
            System.err.printf("Plain text written to: %s (%d characters)%n", 
                textFile.getName(), text.length());
        }
    }

    private void outputStructuredText(OcrResult result) throws Exception {
        StringBuilder output = new StringBuilder();
        
        output.append("=== OCR Results ===\n");
        output.append(String.format("Lines: %d%n", result.lines().size()));
        output.append(String.format("Words: %d%n", result.lines().stream().mapToInt(l -> l.words().size()).sum()));
        
        if (result.textAngle() != 0) {
            output.append(String.format("Text angle: %.1f degrees%n", result.textAngle()));
        }
        output.append("\n");

        for (int i = 0; i < result.lines().size(); i++) {
            OcrLine line = result.lines().get(i);
            output.append(String.format("Line %d: \"%s\"%n", i + 1, line.text()));

            if (showConfidence && !line.words().isEmpty()) {
                output.append("  Words: ");
                for (OcrWord word : line.words()) {
                    output.append(String.format("\"%s\"(%.2f) ", word.text(), word.confidence()));
                }
                output.append("\n");
            }

            if (line.boundingBox() != null && verbose) {
                BoundingBox bbox = line.boundingBox();
                output.append(String.format("  Bounds: (%.0f,%.0f)-(%.0f,%.0f)%n",
                    Math.min(bbox.x1(), bbox.x4()), Math.min(bbox.y1(), bbox.y2()),
                    Math.max(bbox.x2(), bbox.x3()), Math.max(bbox.y3(), bbox.y4())));
            }
            output.append("\n");
        }

        if (outputFile != null) {
            Files.writeString(outputFile.toPath(), output.toString());
        } else {
            System.out.print(output.toString());
        }
    }


    private void outputCompactJson(OcrResult result, File compactJsonFile, int imageWidth, int imageHeight) throws Exception {
        var compactJson = CompactJsonSerializer.toCompactJson(result, inputFile.toPath().getFileName().toString(), imageWidth, imageHeight);
        Files.writeString(compactJsonFile.toPath(), compactJson);
        
        if (verbose) {
            System.err.printf("Compact JSON written to: %s (%d bytes)%n", 
                compactJsonFile.getName(), compactJson.length());
        }
    }

    private void generateSvg(OcrResult result, File svgFile, int imageWidth, int imageHeight) throws Exception {
        String svg = SvgVisualizer.createSvgVisualization(result, inputFile.toPath(), imageWidth, imageHeight);
        Files.writeString(svgFile.toPath(), svg);
    }

    private OcrResult filterByConfidence(OcrResult result, double minConfidence) {
        var filteredLines = result.lines().stream()
            .map(line -> {
                var filteredWords = line.words().stream()
                    .filter(word -> word.confidence() >= minConfidence)
                    .toList();
                
                if (filteredWords.isEmpty()) {
                    return null;
                }
                
                String filteredText = filteredWords.stream()
                    .map(OcrWord::text)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
                
                return new OcrLine(filteredText, line.boundingBox(), filteredWords);
            })
            .filter(line -> line != null)
            .toList();

        String filteredFullText = filteredLines.stream()
            .map(OcrLine::text)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");

        return new OcrResult(filteredFullText, result.textAngle(), filteredLines);
    }

    private static byte[] convertToBGRA(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] bgraData = new byte[width * height * 4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int offset = (y * width + x) * 4;

                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                bgraData[offset] = (byte) blue;
                bgraData[offset + 1] = (byte) green;
                bgraData[offset + 2] = (byte) red;
                bgraData[offset + 3] = (byte) alpha;
            }
        }

        return bgraData;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}