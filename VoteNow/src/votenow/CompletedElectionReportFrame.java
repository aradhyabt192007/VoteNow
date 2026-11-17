package votenow;

import votenow.Models.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class CompletedElectionReportFrame extends JFrame {
    private final User moderator;
    private final Election election;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);
    private final List<JButton> tabButtons = new ArrayList<>();

    private int totalVotes;
    private int totalRegistered;
    private List<CandidateVoteCount> partyStats = new ArrayList<>();
    private List<ZoneTurnout> turnoutStats = new ArrayList<>();
    private List<DemographicPartyCount> areaStats = new ArrayList<>();
    private List<DemographicPartyCount> genderStats = new ArrayList<>();

    public CompletedElectionReportFrame(User moderator, Election election) {
        this.moderator = moderator;
        this.election = election;

        setTitle("VoteNow — Completed Election Report");
        setSize(980, 740);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        // Top Bar Actions
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton exportBtn = Theme.createSuccessButton("Export CSV");
        exportBtn.addActionListener(e -> exportCsvReport());

        JButton themeBtn = Theme.createHeaderButton(Theme.getThemeToggleLabel());
        themeBtn.addActionListener(e -> {
            Theme.toggleTheme();
            dispose();
            new CompletedElectionReportFrame(moderator, election).setVisible(true);
        });

        JButton backBtn = Theme.createHeaderButton("<- Dashboard");
        backBtn.addActionListener(e -> {
            dispose();
            new ModeratorHomeFrame(moderator).setVisible(true);
        });

        topActions.add(exportBtn);
        topActions.add(themeBtn);
        topActions.add(backBtn);

        root.add(Theme.createTopBar(election.title, "Completed Election Analytics — Turnout & Demographics", topActions), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Theme.BG_CANVAS);
        body.setBorder(new EmptyBorder(18, 24, 22, 24));

        try {
            totalVotes = VotingDAO.getTotalVotesForElection(election.electionId);
            totalRegistered = VotingDAO.getTotalRegisteredVoters(election.targetArea);
            partyStats = VotingDAO.getPartyBreakdown(election.electionId);
            turnoutStats = VotingDAO.getZoneTurnout(election.electionId, election.targetArea);
            areaStats = VotingDAO.getAreaBreakdown(election.electionId);
            genderStats = VotingDAO.getGenderBreakdown(election.electionId);

            double turnoutPct = totalRegistered > 0 ? (totalVotes * 100.0) / totalRegistered : 0.0;
            String winnerText = partyStats.isEmpty()
                    ? "No votes cast"
                    : partyStats.get(0).partyName + " (" + partyStats.get(0).votes + ")";

            JPanel summaryGrid = new JPanel(new GridLayout(1, 3, 14, 0));
            summaryGrid.setOpaque(false);

            summaryGrid.add(makeKpiCard("TOTAL VOTES CAST", String.valueOf(totalVotes), Theme.BG_CARD, Theme.BORDER_LIGHT, Theme.TEXT_SECONDARY, Theme.TEXT_PRIMARY));
            summaryGrid.add(makeKpiCard("ELIGIBLE TURNOUT", String.format("%.1f%% (%d / %d)", turnoutPct, totalVotes, totalRegistered), Theme.BG_SELECTED, Theme.PRIMARY, Theme.PRIMARY, Theme.PRIMARY));
            summaryGrid.add(makeKpiCard("LEADING PARTY OVERALL", winnerText, Theme.SUCCESS_BG, Theme.SUCCESS, Theme.SUCCESS, Theme.SUCCESS));

            JPanel analyticsSection = new JPanel(new BorderLayout(0, 12));
            analyticsSection.setOpaque(false);

            JPanel tabBar = new JPanel(new GridLayout(1, 4, 10, 0));
            tabBar.setOpaque(false);

            cardContainer.setOpaque(false);
            cardContainer.add(buildPartyPanel(), "PARTY");
            cardContainer.add(buildTurnoutPanel(), "TURNOUT");
            cardContainer.add(buildDemographicPanel("Area", areaStats), "AREA");
            cardContainer.add(buildDemographicPanel("Gender", genderStats), "GENDER");

            tabBar.add(createTabButton("1. Party Results", "PARTY", 0));
            tabBar.add(createTabButton("2. Turnout by Zone", "TURNOUT", 1));
            tabBar.add(createTabButton("3. Area Breakdown", "AREA", 2));
            tabBar.add(createTabButton("4. Gender Breakdown", "GENDER", 3));

            setActiveTab(0);

            analyticsSection.add(tabBar, BorderLayout.NORTH);
            analyticsSection.add(cardContainer, BorderLayout.CENTER);

            body.add(summaryGrid, BorderLayout.NORTH);
            body.add(analyticsSection, BorderLayout.CENTER);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }

        root.add(body, BorderLayout.CENTER);
        add(root);
    }

    private JPanel makeKpiCard(String heading, String value, Color bg, Color borderCol, Color headCol, Color valCol) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 4));
        card.setBackground(bg);
        card.setBorder(Theme.cardBorder(borderCol, 14, 16));

        JLabel h = new JLabel(heading);
        h.setFont(Theme.FONT_SMALL);
        h.setForeground(headCol);

        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 18));
        v.setForeground(valCol);

        card.add(h);
        card.add(v);
        return card;
    }

    private JButton createTabButton(String label, String cardKey, int index) {
        JButton btn = new JButton(label);
        btn.setFont(Theme.FONT_BTN);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            cardLayout.show(cardContainer, cardKey);
            setActiveTab(index);
        });
        tabButtons.add(btn);
        return btn;
    }

    private void setActiveTab(int activeIndex) {
        for (int i = 0; i < tabButtons.size(); i++) {
            JButton btn = tabButtons.get(i);
            if (i == activeIndex) {
                btn.setBackground(Theme.PRIMARY);
                btn.setForeground(Theme.TEXT_ON_DARK);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.PRIMARY, 1),
                        new EmptyBorder(10, 12, 10, 12)
                ));
            } else {
                btn.setBackground(Theme.BG_CARD);
                btn.setForeground(Theme.TEXT_PRIMARY);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1),
                        new EmptyBorder(10, 12, 10, 12)
                ));
            }
        }
    }

    private JPanel buildPartyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_CARD);
        panel.setBorder(Theme.cardBorder());

        String[] cols = {"Party Name", "Candidate", "Votes Cast", "Vote Share (%)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (CandidateVoteCount item : partyStats) {
            double pct = totalVotes > 0 ? (item.votes * 100.0) / totalVotes : 0.0;
            model.addRow(new Object[]{
                    item.partyName,
                    item.candidateName,
                    item.votes,
                    String.format("%.1f%%", pct)
            });
        }

        JTable table = new JTable(model);
        Theme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTurnoutPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_CARD);
        panel.setBorder(Theme.cardBorder());

        String[] cols = {"Residential Zone", "Registered Voters", "Votes Cast", "Turnout Rate (%)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (ZoneTurnout z : turnoutStats) {
            double pct = z.registeredVoters > 0 ? (z.votesCast * 100.0) / z.registeredVoters : 0.0;
            model.addRow(new Object[]{
                    z.area,
                    z.registeredVoters,
                    z.votesCast,
                    String.format("%.1f%%", pct)
            });
        }

        JTable table = new JTable(model);
        Theme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDemographicPanel(String categoryLabel, List<DemographicPartyCount> stats) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Theme.BG_CARD);
        panel.setBorder(Theme.cardBorder());

        Map<String, Integer> maxVotesPerGroup = new LinkedHashMap<>();
        Map<String, List<String>> leadingPartiesPerGroup = new LinkedHashMap<>();

        for (DemographicPartyCount row : stats) {
            int currentMax = maxVotesPerGroup.getOrDefault(row.categoryValue, -1);
            if (row.votes > currentMax) {
                maxVotesPerGroup.put(row.categoryValue, row.votes);
                List<String> leaders = new ArrayList<>();
                leaders.add(row.partyName);
                leadingPartiesPerGroup.put(row.categoryValue, leaders);
            } else if (row.votes == currentMax) {
                leadingPartiesPerGroup.get(row.categoryValue).add(row.partyName);
            }
        }

        JPanel summaryCard = new JPanel(new BorderLayout(0, 6));
        summaryCard.setBackground(Theme.BG_SELECTED);
        summaryCard.setBorder(Theme.cardBorder(Theme.PRIMARY, 12, 16));

        JLabel summaryHeading = new JLabel("MOST VOTED PARTY BY " + categoryLabel.toUpperCase());
        summaryHeading.setFont(Theme.FONT_SMALL);
        summaryHeading.setForeground(Theme.PRIMARY);

        JTextArea highlightArea = new JTextArea();
        highlightArea.setEditable(false);
        highlightArea.setLineWrap(true);
        highlightArea.setWrapStyleWord(true);
        highlightArea.setOpaque(false);
        highlightArea.setFont(Theme.FONT_BTN);
        highlightArea.setForeground(Theme.TEXT_PRIMARY);

        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : leadingPartiesPerGroup.entrySet()) {
            String group = entry.getKey();
            String parties = String.join(" & ", entry.getValue());
            int votes = maxVotesPerGroup.get(group);
            summary.append(String.format("•  %s:  %s (%d vote%s)\n",
                    group, parties, votes, votes == 1 ? "" : "s"));
        }
        highlightArea.setText(summary.toString().trim());

        summaryCard.add(summaryHeading, BorderLayout.NORTH);
        summaryCard.add(highlightArea, BorderLayout.CENTER);

        String[] cols = {categoryLabel, "Party Name", "Votes Cast", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (DemographicPartyCount row : stats) {
            boolean isLeader = row.votes == maxVotesPerGroup.getOrDefault(row.categoryValue, -1);
            model.addRow(new Object[]{
                    row.categoryValue,
                    row.partyName,
                    row.votes,
                    isLeader ? "Most Voted in " + row.categoryValue : "—"
            });
        }

        JTable table = new JTable(model);
        Theme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        panel.add(summaryCard, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void exportCsvReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Election Analytics Report");
        String safeFileName = election.title.replaceAll("[^a-zA-Z0-9]", "_") + "_Report.csv";
        chooser.setSelectedFile(new File(safeFileName));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("VoteNow Official Election Report," + escapeCsv(election.title));
            out.println("Target Zone," + escapeCsv(election.targetArea));
            out.println("Total Votes Cast," + totalVotes);
            out.println("Eligible Registered Voters," + totalRegistered);
            out.println();

            out.println("PARTY-WISE BREAKDOWN");
            out.println("Party Name,Candidate,Votes Cast,Vote Share (%)");
            for (CandidateVoteCount c : partyStats) {
                double pct = totalVotes > 0 ? (c.votes * 100.0) / totalVotes : 0.0;
                out.printf("%s,%s,%d,%.1f%%%n", escapeCsv(c.partyName), escapeCsv(c.candidateName), c.votes, pct);
            }
            out.println();

            out.println("VOTER TURNOUT BY ZONE");
            out.println("Zone,Registered Voters,Votes Cast,Turnout (%)");
            for (ZoneTurnout z : turnoutStats) {
                double pct = z.registeredVoters > 0 ? (z.votesCast * 100.0) / z.registeredVoters : 0.0;
                out.printf("%s,%d,%d,%.1f%%%n", escapeCsv(z.area), z.registeredVoters, z.votesCast, pct);
            }
            out.println();

            out.println("AREA BREAKDOWN");
            out.println("Area,Party Name,Votes Cast");
            for (DemographicPartyCount a : areaStats) {
                out.printf("%s,%s,%d%n", escapeCsv(a.categoryValue), escapeCsv(a.partyName), a.votes);
            }
            out.println();

            out.println("GENDER BREAKDOWN");
            out.println("Gender,Party Name,Votes Cast");
            for (DemographicPartyCount g : genderStats) {
                out.printf("%s,%s,%d%n", escapeCsv(g.categoryValue), escapeCsv(g.partyName), g.votes);
            }

            JOptionPane.showMessageDialog(this, "Report exported successfully to:\n" + file.getAbsolutePath(), "VoteNow — Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save CSV file: " + ex.getMessage(), "VoteNow — Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}