package com.passgen;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Collects entropy from mouse movement events.
 * <p>
 * When the user moves the mouse over the UI, the harvester captures
 * (x, y, nanoTime) tuples, serialises them to bytes, and periodically
 * feeds them into the {@link PasswordEngine}'s SecureRandom instance.
 * <p>
 * This is <b>supplementary</b> entropy — SecureRandom already uses the
 * OS entropy pool, but mouse jitter adds an extra, user-observable
 * source of randomness.
 */
public class EntropyHarvester extends MouseMotionAdapter {

    /** Flush collected entropy to the engine after this many events */
    private static final int FLUSH_THRESHOLD = 50;

    private final PasswordEngine engine;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private int eventCount = 0;
    private boolean active = false;

    /** Label that displays how many entropy events have been collected */
    private JLabel statusLabel;
    private int totalEvents = 0;

    public EntropyHarvester(PasswordEngine engine) {
        this.engine = engine;
    }

    /**
     * Sets the label that will display entropy collection status.
     */
    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }

    /**
     * Enable or disable the harvester.
     */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            flushEntropy();  // flush any remaining data when deactivated
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!active) return;
        collectEvent(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!active) return;
        collectEvent(e.getX(), e.getY());
    }

    /**
     * Serialises one mouse event into the buffer.
     */
    private void collectEvent(int x, int y) {
        try {
            DataOutputStream dos = new DataOutputStream(buffer);
            dos.writeInt(x);
            dos.writeInt(y);
            dos.writeLong(System.nanoTime());
        } catch (IOException ignored) {
            // ByteArrayOutputStream never throws IOException
        }

        eventCount++;
        totalEvents++;

        // Update the status label on the EDT
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() ->
                    statusLabel.setText(String.format("Entropy: %d events collected", totalEvents)));
        }

        // Flush when we've accumulated enough events
        if (eventCount >= FLUSH_THRESHOLD) {
            flushEntropy();
        }
    }

    /**
     * Feeds the accumulated entropy bytes into the engine and resets the buffer.
     */
    private void flushEntropy() {
        if (buffer.size() > 0) {
            engine.addEntropySeed(buffer.toByteArray());
            buffer.reset();
            eventCount = 0;
        }
    }

    /**
     * Returns the total number of mouse events collected since creation.
     */
    public int getTotalEvents() {
        return totalEvents;
    }
}
