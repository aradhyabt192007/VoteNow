package votenow;

import votenow.Models.Election;
import votenow.Models.User;
import votenow.Models.VoteReceipt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VoterHomeFrame extends JFrame {
    private final User voter;
    private final Map<Election, JLabel> upcomingCountdownLabels = new LinkedHashMap<>();
    private final Timer upcomingTimer;

    public VoterHomeFrame(User voter) {
        this.voter = voter;
        setTitle("VoteNow — Voter Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        // Top Bar Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton receiptsBtn = Theme.createPrimaryButton("My Vote Receipts");
        receiptsBtn.addActionListener(e -> showReceiptsDialog());

        JButton themeBtn = Theme.createHeaderButton(Theme.getThemeToggleLabel());
        themeBtn.addActionListener(e -> {
            stopTimer();
            Theme.toggleTheme();
            dispose();
            new VoterHomeFrame(voter).setVisible(true);
        });

        JButton logoutBtn = Theme.createHeaderButton("Logout");
        logoutBtn.addActionListener(e -> {
            stopTimer();
            dispose();
            new LoginFrame().setVisible(true);
        });

        actions.add(receiptsBtn);
        actions.add(themeBtn);
        actions.add(logoutBtn);

        String sub = "Voter: " + voter.fullName + "  •  Zone: " + voter.area + "  •  Gender: " + voter.gender;
        root.add(Theme.createTopBar("VoteNow — Citizen Portal", sub, actions), BorderLayout.NORTH);

        // Center Content
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.BG_CANVAS);
        listPanel.setBorder(new EmptyBorder(20, 24, 24, 24));

        try {
            // Section 1: In Progress (Filtered to ALL or voter.area)
            JLabel activeTitle = new JLabel("ELECTIONS IN PROGRESS (ELIGIBLE FOR " + voter.area.toUpperCase() + ")");
            activeTitle.setFont(Theme.FONT_SECTION);
            activeTitle.setForeground(Theme.SUCCESS);
            activeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(activeTitle);
            listPanel.add(Box.createRigidArea(new Dimension(0, 12)));

            List<Election> activeElections = VotingDAO.getElectionsForVoter("IN_PROGRESS", voter.area);
            if (activeElections.isEmpty()) {
                JLabel empty = new JLabel("No elections are currently open for voting in your zone.");
                empty.setFont(Theme.FONT_BODY);
                empty.setForeground(Theme.TEXT_SECONDARY);
                listPanel.add(empty);
            } else {
                for (Election election : activeElections) {
                    JPanel card = createElectionCard(election, true);
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(card);
                    listPanel.add(Box.createRigidArea(new Dimension(0, 12)));
                }
            }

            // Section 2: Upcoming Scheduled Elections
            List<Election> upcomingElections = VotingDAO.getElectionsForVoter("UPCOMING", voter.area);
            if (!upcomingElections.isEmpty()) {
                listPanel.add(Box.createRigidArea(new Dimension(0, 14)));
                JLabel upTitle = new JLabel("UPCOMING SCHEDULED ELECTIONS");
                upTitle.setFont(Theme.FONT_SECTION);
                upTitle.setForeground(Theme.WARNING);
                upTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(upTitle);
                listPanel.add(Box.createRigidArea(new Dimension(0, 12)));

                for (Election election : upcomingElections) {
                    JPanel card = createElectionCard(election, false);
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(card);
                    listPanel.add(Box.createRigidArea(new Dimension(0, 12)));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading elections: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }

        root.add(Theme.createCleanScrollPane(listPanel), BorderLayout.CENTER);
        add(root);

        upcomingTimer = new Timer(1000, e -> tickUpcomingCountdowns());
        if (!upcomingCountdownLabels.isEmpty()) {
            tickUpcomingCountdowns();
            upcomingTimer.start();
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopTimer();
            }
        });
    }

    private void stopTimer() {
        if (upcomingTimer != null && upcomingTimer.isRunning()) {
            upcomingTimer.stop();
        }
    }

    private void tickUpcomingCountdowns() {
        boolean anyOpened = false;
        for (Map.Entry<Election, JLabel> entry : upcomingCountdownLabels.entrySet()) {
            Duration rem = Duration.between(LocalDateTime.now(), entry.getKey().startTime);
            if (rem.isNegative() || rem.isZero()) {
                anyOpened = true;
            } else {
                long s = rem.getSeconds();
                entry.getValue().setText(String.format("  Opens in %02dh %02dm %02ds  ", s / 3600, (s % 3600) / 60, s % 60));
            }
        }
        if (anyOpened) {
            stopTimer();
            dispose();
            new VoterHomeFrame(voter).setVisible(true);
        }
    }

    private JPanel createElectionCard(Election election, boolean isActive) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(Theme.cardBorder());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        textPanel.setOpaque(false);

        String zoneTag = "ALL".equalsIgnoreCase(election.targetArea) ? "All Zones" : election.targetArea + " Only";
        JLabel title = new JLabel(election.title + "  [" + zoneTag + "]");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel desc = new JLabel(election.description);
        desc.setFont(Theme.FONT_BODY);
        desc.setForeground(Theme.TEXT_SECONDARY);

        textPanel.add(title);
        textPanel.add(desc);

        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setOpaque(false);

        if (!isActive) {
            JLabel countdownBadge = new JLabel("  Scheduled  ", SwingConstants.CENTER);
            countdownBadge.setFont(Theme.FONT_BTN);
            countdownBadge.setOpaque(true);
            countdownBadge.setBackground(Theme.WARNING_BG);
            countdownBadge.setForeground(Theme.WARNING);
            countdownBadge.setBorder(Theme.cardBorder(Theme.WARNING, 8, 12));
            upcomingCountdownLabels.put(election, countdownBadge);
            rightWrap.add(countdownBadge);
        } else {
            boolean alreadyVoted = false;
            try {
                alreadyVoted = VotingDAO.hasVoterVoted(election.electionId, voter.userId);
            } catch (SQLException ignored) {
            }

            if (alreadyVoted) {
                JLabel votedBadge = new JLabel("  ✓ Vote Recorded  ", SwingConstants.CENTER);
                votedBadge.setFont(Theme.FONT_BTN);
                votedBadge.setOpaque(true);
                votedBadge.setBackground(Theme.SUCCESS_BG);
                votedBadge.setForeground(Theme.SUCCESS);
                votedBadge.setBorder(Theme.cardBorder(Theme.SUCCESS, 8, 14));
                rightWrap.add(votedBadge);
            } else {
                JButton voteBtn = Theme.createSuccessButton("Cast Vote  ->");
                voteBtn.addActionListener(e -> {
                    stopTimer();
                    dispose();
                    new VotingFrame(voter, election).setVisible(true);
                });
                rightWrap.add(voteBtn);
            }
        }

        card.add(textPanel, BorderLayout.CENTER);
        card.add(rightWrap, BorderLayout.EAST);
        return card;
    }

    private void showReceiptsDialog() {
        try {
            List<VoteReceipt> receipts = VotingDAO.getVoterReceipts(voter.userId);

            JDialog dialog = new JDialog(this, "VoteNow — My Ballot Receipts", true);
            dialog.setSize(660, 420);
            dialog.setLocationRelativeTo(this);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Theme.BG_CANVAS);
            root.add(Theme.createTopBar("My Ballot Receipts", "Verified participation codes (Candidate choice remains secret)", null), BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout());
            center.setBackground(Theme.BG_CANVAS);
            center.setBorder(new EmptyBorder(18, 20, 18, 20));

            DefaultTableModel model = new DefaultTableModel(new String[]{"Election", "Confirmation Receipt Code", "Timestamp"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };

            for (VoteReceipt r : receipts) {
                model.addRow(new Object[]{r.electionTitle, r.receiptCode, r.votedAt});
            }

            JTable table = new JTable(model);
            Theme.styleTable(table);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
            scroll.getViewport().setBackground(Theme.BG_CARD);

            center.add(scroll, BorderLayout.CENTER);
            root.add(center, BorderLayout.CENTER);
            dialog.add(root);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading receipts: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}