package com.passgen;

import javax.swing.*;
import java.awt.*;

/**
 * Visual password strength meter panel.
 * <p>
 * Displays a color-coded progress bar with strength tier label,
 * entropy bits, and estimated time-to-crack.
 */
public class StrengthMeterPanel extends JPanel {

    private final JProgressBar strengthBar;
    private final JLabel tierLabel;
    private final JLabel entropyLabel;
    private final JLabel crackTimeLabel;

    // Colour constants for each tier
    private static final Color COLOR_NONE       = new Color(100, 100, 100);
    private static final Color COLOR_WEAK       = new Color(220, 50, 50);
    private static final Color COLOR_FAIR       = new Color(240, 160, 40);
    private static final Color COLOR_STRONG     = new Color(60, 180, 75);
    private static final Color COLOR_VERY_STRONG = new Color(30, 220, 80);

    public StrengthMeterPanel() {
        setLayout(new BorderLayout(8, 4));
        setBorder(BorderFactory.createTitledBorder("Password Strength"));

        // ── Progress bar ─────────────────────────────────────────
        strengthBar = new JProgressBar(0, 128);
        strengthBar.setValue(0);
        strengthBar.setStringPainted(true);
        strengthBar.setString("");
        strengthBar.setPreferredSize(new Dimension(0, 22));
        add(strengthBar, BorderLayout.NORTH);

        // ── Info labels panel ────────────────────────────────────
        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 8, 0));

        tierLabel = new JLabel("Tier: —", SwingConstants.LEFT);
        tierLabel.setFont(tierLabel.getFont().deriveFont(Font.BOLD, 13f));

        entropyLabel = new JLabel("Entropy: — bits", SwingConstants.CENTER);
        entropyLabel.setFont(entropyLabel.getFont().deriveFont(12f));

        crackTimeLabel = new JLabel("Crack time: —", SwingConstants.RIGHT);
        crackTimeLabel.setFont(crackTimeLabel.getFont().deriveFont(12f));

        infoPanel.add(tierLabel);
        infoPanel.add(entropyLabel);
        infoPanel.add(crackTimeLabel);

        add(infoPanel, BorderLayout.CENTER);
    }

    /**
     * Updates the meter with fresh entropy analysis.
     *
     * @param result the entropy result from PasswordEngine
     */
    public void update(PasswordEngine.EntropyResult result) {
        // Clamp bar value to 0–128
        int barValue = (int) Math.min(result.bits, 128);
        strengthBar.setValue(barValue);
        strengthBar.setString(String.format("%.1f bits", result.bits));

        // Tier-specific colour
        Color barColor;
        switch (result.tier) {
            case WEAK:        barColor = COLOR_WEAK;        break;
            case FAIR:        barColor = COLOR_FAIR;        break;
            case STRONG:      barColor = COLOR_STRONG;      break;
            case VERY_STRONG: barColor = COLOR_VERY_STRONG; break;
            default:          barColor = COLOR_NONE;        break;
        }
        strengthBar.setForeground(barColor);

        tierLabel.setText("Tier: " + result.tier.getLabel());
        tierLabel.setForeground(barColor);
        entropyLabel.setText(String.format("Entropy: %.1f bits", result.bits));
        crackTimeLabel.setText("Crack time: " + result.timeToCrack);
    }

    /**
     * Resets the meter to its initial empty state.
     */
    public void reset() {
        strengthBar.setValue(0);
        strengthBar.setString("");
        strengthBar.setForeground(COLOR_NONE);
        tierLabel.setText("Tier: —");
        tierLabel.setForeground(UIManager.getColor("Label.foreground"));
        entropyLabel.setText("Entropy: — bits");
        crackTimeLabel.setText("Crack time: —");
    }
}
