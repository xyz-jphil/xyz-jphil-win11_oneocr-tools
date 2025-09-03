package xyz.jphil.win11_oneocr.tools.folder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WorkQueue {
    private final BlockingQueue<WorkItem> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean discoveryComplete = new AtomicBoolean(false);
    
    // Total metrics (discovered during scope discovery)
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicLong totalPages = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    
    // Completed metrics (updated as work progresses)
    private final AtomicInteger completedFiles = new AtomicInteger(0);
    private final AtomicLong completedPages = new AtomicLong(0);
    private final AtomicLong completedBytes = new AtomicLong(0);
    
    // Reliability flags
    private final AtomicInteger pagesFromActualCount = new AtomicInteger(0);
    private final AtomicInteger pagesFromEstimate = new AtomicInteger(0);
    
    public void addWork(WorkItem item) {
        queue.offer(item);
        totalFiles.incrementAndGet();
        totalPages.addAndGet(item.estimatedPages());
        totalBytes.addAndGet(item.fileSizeBytes());
        
        // Track page count reliability
        if (item.isPageCountActual()) {
            pagesFromActualCount.addAndGet(item.estimatedPages());
        } else {
            pagesFromEstimate.addAndGet(item.estimatedPages());
        }
    }
    
    public WorkItem takeWork() throws InterruptedException {
        return queue.take();
    }
    
    public boolean hasWork() {
        return !queue.isEmpty() || !discoveryComplete.get();
    }
    
    public void markDiscoveryComplete() {
        discoveryComplete.set(true);
    }
    
    public boolean isDiscoveryComplete() {
        return discoveryComplete.get();
    }
    
    public int getTotalFiles() {
        return totalFiles.get();
    }
    
    public long getTotalPages() {
        return totalPages.get();
    }
    
    public long getTotalBytes() {
        return totalBytes.get();
    }
    
    public int getCompletedFiles() {
        return completedFiles.get();
    }
    
    public long getCompletedPages() {
        return completedPages.get();
    }
    
    public long getCompletedBytes() {
        return completedBytes.get();
    }
    
    public void markWorkCompleted(WorkItem item, int actualPagesProcessed) {
        completedFiles.incrementAndGet();
        completedPages.addAndGet(actualPagesProcessed);
        completedBytes.addAndGet(item.fileSizeBytes());
    }
    
    public double getPageCountReliability() {
        long total = pagesFromActualCount.get() + pagesFromEstimate.get();
        return total > 0 ? (double) pagesFromActualCount.get() / total : 0.0;
    }
    
    public ProgressMetrics getProgressMetrics() {
        return new ProgressMetrics(
            totalFiles.get(), completedFiles.get(),
            totalPages.get(), completedPages.get(), 
            totalBytes.get(), completedBytes.get(),
            getPageCountReliability()
        );
    }
    
    public int getQueueSize() {
        return queue.size();
    }
}