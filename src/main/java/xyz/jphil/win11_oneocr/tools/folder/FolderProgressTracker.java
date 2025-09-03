package xyz.jphil.win11_oneocr.tools.folder;

import xyz.jphil.win11_oneocr.tools.ProgressTracker;
import xyz.jphil.win11_oneocr.tools.DualProgressRenderer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Folder-mode ProgressTracker with dynamic total support.
 * Registers with DualProgressRenderer for coordinated display.
 */
public class FolderProgressTracker extends ProgressTracker {
    private final AtomicInteger dynamicTotal;
    private final String trackerId;
    
    public FolderProgressTracker(String task, int initialTotal, boolean verbose, String trackerId) {
        super(task, initialTotal, verbose);
        this.dynamicTotal = new AtomicInteger(initialTotal);
        this.trackerId = trackerId;
        
        // Register with dual progress renderer
        DualProgressRenderer.register(trackerId, this);
    }
    
    public void updateTotal(int newTotal) {
        dynamicTotal.set(Math.max(newTotal, dynamicTotal.get()));
    }
    
    @Override
    public int total() {
        return dynamicTotal.get();
    }
    
    @Override
    public String toString() {
        // Use different visual style for folder progress (vs PDF progress)
        // Access completed count through reflection or use a different approach
        String superToString = super.toString();
        int currentTotal = dynamicTotal.get();
        
        // Extract completed count from super's calculation
        int currentCompleted = extractCompletedFromSuper();
        if (currentCompleted < 0) {
            // Fallback to super's rendering if we can't extract
            return superToString.replace("█", "▓"); // Just change the visual
        }
        
        if (currentTotal == 0) {
            return String.format("[%s] %s", renderFolderProgressBar(0.0), "0.0%");
        }
        
        double pct = (double) currentCompleted / currentTotal * 100;
        
        return String.format("[%s] %5.1f%% (%d/%d) ETA: %s Rate: %s", 
            renderFolderProgressBar(pct / 100.0), pct, currentCompleted, currentTotal,
            "calculating...", "calculating...");
    }
    
    private String renderFolderProgressBar(double progress) {
        int width = 25;
        int filled = (int) Math.round(progress * width);
        // Use different characters for folder progress: ▓ (filled) and ░ (empty)
        return "▓".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, width - filled));
    }
    
    private String formatDuration(java.time.Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
    
    private int extractCompletedFromSuper() {
        // Simple approach - use reflection to access the completed field
        try {
            var field = ProgressTracker.class.getDeclaredField("completed");
            field.setAccessible(true);
            return ((java.util.concurrent.atomic.AtomicInteger) field.get(this)).get();
        } catch (Exception e) {
            return -1; // Signal failure
        }
    }
    
    @Override
    public ProgressTracker inc() {
        super.inc();
        // Trigger coordinated rendering in folder mode
        if (DualProgressRenderer.isFolderModeActive()) {
            DualProgressRenderer.renderAll();
        }
        return this;
    }
    
    @Override
    public ProgressTracker done() {
        var result = super.done();
        DualProgressRenderer.unregister(trackerId);
        return result;
    }
}