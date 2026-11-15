package io.akshat.tvsearch;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

/** Observes only Google's TV search results. Never requests or filters remote keys. */
public final class SearchHandoffService extends AccessibilityService {
    private static final String GOOGLE = "com.google.android.katniss";
    private static final String QUERY_ID = GOOGLE + ":id/super_header";
    private static final String TARGET = "io.gh.akshat.tizentube.cobalt";
    private static final String FALLBACK = "io.gh.reisxd.tizentube.cobalt";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean forwarded;
    private String lastQuery;
    private long lastForwardedAt;
    private int pollsRemaining;
    private final Runnable inspect = this::poll;

    private void poll() {
        forwardResults();
        if (!forwarded && --pollsRemaining > 0) handler.postDelayed(inspect, 250);
    }

    @Override protected void onServiceConnected() {
        pollsRemaining = 40;
        handler.postDelayed(inspect, 200);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (TARGET.contentEquals(event.getPackageName()) || FALLBACK.contentEquals(event.getPackageName())) {
            forwarded = false;
            handler.removeCallbacks(inspect);
            return;
        }
        if (!GOOGLE.contentEquals(event.getPackageName())) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            forwarded = false;
        }
        // Read immediately, then retry a content update if the header has not populated yet.
        forwardResults();
        handler.removeCallbacks(inspect);
        if (!forwarded) {
            pollsRemaining = 40;
            handler.postDelayed(inspect, 200);
        }
    }

    private void forwardResults() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            List<AccessibilityWindowInfo> windows = getWindows();
            for (AccessibilityWindowInfo window : windows) {
                AccessibilityNodeInfo candidate = window.getRoot();
                if (candidate != null) {
                    if (root == null && GOOGLE.contentEquals(candidate.getPackageName())) root = candidate;
                    else candidate.recycle();
                }
                window.recycle();
            }
        }
        if (root == null) return;
        try {
            if (!GOOGLE.contentEquals(root.getPackageName())) return;
            List<AccessibilityNodeInfo> headers = root.findAccessibilityNodeInfosByViewId(QUERY_ID);
            try {
                if (headers.isEmpty() || headers.get(0).getText() == null) return;
                String query = headers.get(0).getText().toString().trim();
                if (query.isEmpty()) return;
                if (query.equals(lastQuery) && (forwarded || SystemClock.elapsedRealtime() - lastForwardedAt < 2000)) return;
                Intent search = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results").buildUpon()
                                .appendQueryParameter("search_query", query).build())
                        .setPackage(TARGET)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (search.resolveActivity(getPackageManager()) == null) search.setPackage(FALLBACK);
                if (search.resolveActivity(getPackageManager()) == null) {
                    forwarded = true;
                    Log.w("TvSearchHandoff", "TizenTube unavailable");
                    return;
                }
                // Mark before launch: repeated window/content events must not launch duplicates.
                forwarded = true;
                lastQuery = query;
                lastForwardedAt = SystemClock.elapsedRealtime();
                // Dismiss Google's dead-end results so Back cannot bounce through them indefinitely.
                performGlobalAction(GLOBAL_ACTION_BACK);
                try {
                    startActivity(search);
                    Log.i("TvSearchHandoff", "Opened TizenTube search");
                } catch (ActivityNotFoundException | SecurityException e) {
                    // Do not trap the user in an automatic retry loop.
                    Log.w("TvSearchHandoff", "TizenTube unavailable");
                }
            } finally {
                for (AccessibilityNodeInfo node : headers) node.recycle();
            }
        } finally {
            root.recycle();
        }
    }
    @Override public void onInterrupt() { handler.removeCallbacks(inspect); }
    @Override public void onDestroy() { handler.removeCallbacks(inspect); super.onDestroy(); }
}
