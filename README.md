# Windows 11 OneOCR Tools

Command-line tools and output formatters for Windows 11 OneOCR using Java FFM (Foreign Function & Memory API).

## Requirements
- **API**: [xyz-jphil-win11_oneocr-api](https://github.com/xyz-jphil/xyz-jphil-win11_oneocr-api)
- **JDK 22+** (recommended) or JDK 21 with `--enable-preview` for runtime
- Windows 11

## Build
```bash
mvn clean package
```

## Usage
```bash
# Basic OCR (with informative verbose mode)
java --enable-native-access=ALL-UNNAMED -jar target/xyz-jphil-win11_oneocr-tools-1.0.jar -v image.jpg

# Custom output files
java --enable-native-access=ALL-UNNAMED -jar target/xyz-jphil-win11_oneocr-tools-1.0.jar --json output.json --svg output.svg --text output.txt image.jpg 

# Confidence filtering and verbose output
java --enable-native-access=ALL-UNNAMED -jar target/xyz-jphil-win11_oneocr-tools-1.0.jar --min-confidence 0.8 --verbose image.jpg 
```

## Features
- JSON export with metadata
- SVG visualization with bounding boxes
- Plain text extraction
- Command-line interface

## Notes 
- Windows 11 Snipping tools OneOCR model (win11-oneocr) is the best price/quality/speed trade-off you can get for OCR. It's quality and multi-language OCR capabilities are state-of-art. 
- The win11-oneocr - is quiet light-weight, just 50MB dll and 50MB model, it does 1 page in 1 to 2 seconds giving a rate of 30K-50K pages per day using pure CPU (utilizing all cores) without requiring GPU. 
- The fact that you don't need a GPU is both a pro and con. Not able to leverage GPU limits scaling.  
- This tools module is however just an example implementation of the api, and serves as a fairly decent standalone simple tool. It lacks batch processing which is essential, otherwise for each image you would be re-loading the model each time which has a performance hit if you are planning to do millions of pages OCR with this.
- However there are examples which demonstrate how to load model once and do images in batch thus utilizing full CPU ( see TestJDK21JDK22Plus.java in api module). Batch processing in command line will be implemented soon.
- GraalVM native executable version is planned for future releases to simplify deployment, eliminate JDK dependency.
- To use this, you must first build the api module. We DO NOT provide pre-build jars or exes.

## Related
- **API**: [xyz-jphil-win11_oneocr-api](https://github.com/xyz-jphil/xyz-jphil-win11_oneocr-api) (includes one-ocr dlls packaged).
- **Original**: [win11-oneocr](https://github.com/b1tg/win11-oneocr)