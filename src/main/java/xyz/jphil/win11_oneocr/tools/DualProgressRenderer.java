package xyz.jphil.win11_oneocr.tools;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Folder-mode only: Renders multiple progress bars simultaneously.
 * Does not affect single PDF processing mode.
 */
public class DualProgressRenderer {
    private static volatile boolean folderModeActive = false;
    private static final Map<String, ProgressTracker> activeTrackers = new ConcurrentHashMap<>();
    private static final Object renderLock = new Object();
    
    public static void enableFolderMode() {
        folderModeActive = true;
    }
    
    public static void disableFolderMode() {
        folderModeActive = false;
        activeTrackers.clear();
    }
    
    public static boolean isFolderModeActive() {
        return folderModeActive;
    }
    
    public static void register(String id, ProgressTracker tracker) {
        if (folderModeActive) {
            activeTrackers.put(id, tracker);
        }
    }
    
    public static void unregister(String id) {
        activeTrackers.remove(id);
    }
    
    /**
     * Render all active progress bars simultaneously.
     * Called whenever any progress bar needs to update.
     */
    public static void renderAll() {
        if (!folderModeActive) return; // Only render in folder mode
        
        synchronized (renderLock) {
            if (activeTrackers.isEmpty()) return;
            
            // Clear lines for all active progress bars
            int linesToClear = activeTrackers.size();
            for (int i = 0; i < linesToClear; i++) {
                System.err.print("\r\033[K\033[1A"); // Clear line and move up
            }
            
            // Render all progress bars in consistent order
            var trackerList = new ArrayList<>(activeTrackers.values());
            trackerList.sort((a, b) -> a.task().compareTo(b.task())); // Consistent ordering
            
            for (var tracker : trackerList) {
                renderProgressLine(tracker);
            }
        }
    }
    
    private static void renderProgressLine(ProgressTracker tracker) {
        if (tracker.total() == 0 || !tracker.showProgress()) return;
        
        var stats = tracker.calcStats();
        var currentCompleted = tracker.completed().get();
        var bar = progressBar(currentCompleted, tracker.total());
        
        if (tracker.verbose()) {
            System.err.printf("[%s] %5.1f%% (%d/%d) %s %s%n",
                bar, stats.pct(), currentCompleted, tracker.total(), stats.eta(), stats.rate());
        } else {
            System.err.printf("[%s] %5.1f%% (%d/%d)%n",
                bar, stats.pct(), currentCompleted, tracker.total());
        }
    }
    
    private static String progressBar(int completed, int total) {
        var width = 25;
        var progress = Math.min(1.0, (double) completed / total);
        var filled = (int) (width * progress);
        
        var sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append('█');
        for (int i = filled; i < width; i++) sb.append('░');
        return sb.toString();
    }
}