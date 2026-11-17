package votenow;

import votenow.Models.CandidateVoteCount;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class VoteChartPanel extends JPanel {
    private final List<CandidateVoteCount> data;
    private final int totalVotes;

    public VoteChartPanel(List<CandidateVoteCount> data, int totalVotes) {
        this.data = data;
        this.totalVotes = totalVotes;
        setBackground(Theme.BG_CARD);
        setPreferredSize(new Dimension(800, 230));
        setBorder(Theme.cardBorder(Theme.BORDER_LIGHT, 12, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (data == null || data.isEmpty() || totalVotes == 0) {
            g2.setFont(Theme.FONT_BODY);
            g2.setColor(Theme.TEXT_SECONDARY);
            String msg = "No votes recorded yet to visualize.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Left Half: Pie Chart (Vote Share)
        int midX = w / 2;
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("VOTE SHARE DISTRIBUTION (PIE)", 20, 24);

        int diameter = Math.min(midX - 60, h - 60);
        diameter = Math.max(diameter, 90);
        int pieX = 28;
        int pieY = (h - diameter) / 2 + 8;

        double startAngle = 90.0;
        for (int i = 0; i < data.size(); i++) {
            CandidateVoteCount item = data.get(i);
            if (item.votes <= 0) continue;
            double sweep = (item.votes * 360.0) / totalVotes;
            g2.setColor(Theme.CHART_COLORS[i % Theme.CHART_COLORS.length]);
            g2.fill(new Arc2D.Double(pieX, pieY, diameter, diameter, startAngle, -sweep, Arc2D.PIE));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Arc2D.Double(pieX, pieY, diameter, diameter, startAngle, -sweep, Arc2D.PIE));
            startAngle -= sweep;
        }

        // Pie Legend
        int legendX = pieX + diameter + 22;
        int legendY = pieY + 14;
        for (int i = 0; i < data.size(); i++) {
            CandidateVoteCount item = data.get(i);
            Color c = Theme.CHART_COLORS[i % Theme.CHART_COLORS.length];
            g2.setColor(c);
            g2.fillRoundRect(legendX, legendY - 10, 12, 12, 4, 4);

            double pct = (item.votes * 100.0) / totalVotes;
            String label = truncate(item.partyName, 20) + String.format(" (%.0f%%)", pct);
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(label, legendX + 18, legendY);
            legendY += 24;
        }

        // Divider Line
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(midX, 18, midX, h - 18);

        // Right Half: Horizontal Bar Chart (Vote Counts)
        int barAreaX = midX + 24;
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("TOTAL VOTES BY PARTY (BAR)", barAreaX, 24);

        int maxVotes = 1;
        for (CandidateVoteCount item : data) {
            if (item.votes > maxVotes) maxVotes = item.votes;
        }

        int rowY = 46;
        int maxBarWidth = Math.max(80, w - barAreaX - 165);
        for (int i = 0; i < data.size(); i++) {
            CandidateVoteCount item = data.get(i);
            Color c = Theme.CHART_COLORS[i % Theme.CHART_COLORS.length];

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(truncate(item.partyName, 18), barAreaX, rowY + 13);

            int barX = barAreaX + 125;
            // Track background
            g2.setColor(Theme.BG_CANVAS);
            g2.fill(new RoundRectangle2D.Double(barX, rowY, maxBarWidth, 18, 6, 6));

            // Filled bar
            int filledW = (int) ((item.votes * 1.0 / maxVotes) * maxBarWidth);
            if (item.votes > 0 && filledW < 8) filledW = 8;
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(barX, rowY, filledW, 18, 6, 6));

            // Vote count text
            g2.setFont(Theme.FONT_BTN);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(String.valueOf(item.votes), barX + maxBarWidth + 10, rowY + 14);

            rowY += 32;
        }

        g2.dispose();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}