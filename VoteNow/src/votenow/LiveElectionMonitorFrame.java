package votenow;

import votenow.Models.CandidateVoteCount;
import votenow.Models.Election;
import votenow.Models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class LiveElectionMonitorFrame extends JFrame {
    private final User moderator;
    private final Election election;
    private final JLabel totalVotesLabel;
    private final JLabel countdownLabel;
    private final DefaultTableModel tableModel;
    private final Timer refreshTimer;
    private boolean closingHandled = false;

    public LiveElectionMonitorFrame(User moderator, Election election) {
        this.moderator = moderator;
        this.election = election;

        setTitle("VoteNow — Live Monitor (" + election.title + ")");
        setSize(780, 560);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton closePollBtn = Theme.createDangerButton("Close Poll Early");
        closePollBtn.addActionListener(e -> handleManualClose());

        JButton backBtn = Theme.createHeaderButton("<- Dashboard");
        backBtn.addActionListener(e -> closeAndReturn());

        topActions.add(closePollBtn);
        topActions.add(backBtn);

        root.add(Theme.createTopBar(election.title, "Zone: " + election.targetArea + "  •  Real-Time Vote Count & Countdown", topActions), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setBackground(Theme.BG_CANVAS);
        center.setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel metricsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        metricsPanel.setOpaque(false);

        JPanel votesCard = new JPanel(new GridLayout(2, 1, 0, 6));
        votesCard.setBackground(Theme.SUCCESS_BG);
        votesCard.setBorder(Theme.cardBorder(Theme.SUCCESS, 16, 20));

        JLabel vTitle = new JLabel("TOTAL VOTES CAST (LIVE)", SwingConstants.CENTER);
        vTitle.setFont(Theme.FONT_SMALL);
        vTitle.setForeground(Theme.SUCCESS);

        totalVotesLabel = new JLabel("0", SwingConstants.CENTER);
        totalVotesLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        totalVotesLabel.setForeground(Theme.SUCCESS);

        votesCard.add(vTitle);
        votesCard.add(totalVotesLabel);

        JPanel timerCard = new JPanel(new GridLayout(2, 1, 0, 6));
        timerCard.setBackground(Theme.DANGER_BG);
        timerCard.setBorder(Theme.cardBorder(Theme.DANGER, 16, 20));

        JLabel tTitle = new JLabel("TIME REMAINING", SwingConstants.CENTER);
        tTitle.setFont(Theme.FONT_SMALL);
        tTitle.setForeground(Theme.DANGER);

        countdownLabel = new JLabel("00h 00m 00s", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        countdownLabel.setForeground(Theme.DANGER);

        timerCard.add(tTitle);
        timerCard.add(countdownLabel);

        metricsPanel.add(votesCard);
        metricsPanel.add(timerCard);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Theme.BG_CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));

        String[] columns = {"Candidate", "Party", "Live Votes Cast"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tallyTable = new JTable(tableModel);
        Theme.styleTable(tallyTable);

        JScrollPane tableScroll = new JScrollPane(tallyTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Theme.BG_CARD);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        center.add(metricsPanel, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        add(root);

        refreshTimer = new Timer(1000, e -> updateLiveStats());
        updateLiveStats();
        refreshTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                refreshTimer.stop();
            }
        });
    }

    private void updateLiveStats() {
        if (closingHandled) return;

        Duration remaining = Duration.between(LocalDateTime.now(), election.endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            closingHandled = true;
            refreshTimer.stop();
            countdownLabel.setText("00h 00m 00s");
            finalizeElection("Poll timer has ended! Moving election to Completed Analytics.");
            return;
        }

        long totalSeconds = remaining.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        countdownLabel.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));

        try {
            int totalVotes = VotingDAO.getTotalVotesForElection(election.electionId);
            totalVotesLabel.setText(String.valueOf(totalVotes));

            List<CandidateVoteCount> breakdown = VotingDAO.getPartyBreakdown(election.electionId);
            tableModel.setRowCount(0);
            for (CandidateVoteCount row : breakdown) {
                tableModel.addRow(new Object[]{row.candidateName, row.partyName, row.votes});
            }
        } catch (SQLException ex) {
            countdownLabel.setText("DB Sync Error");
        }
    }

    private void handleManualClose() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "End '" + election.title + "' immediately and lock voting?",
                "VoteNow — Close Poll Early",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            closingHandled = true;
            refreshTimer.stop();
            finalizeElection("Election has been closed early. Opening Final Analytics Report.");
        }
    }

    private void finalizeElection(String message) {
        try {
            VotingDAO.completeElection(election.electionId);
            JOptionPane.showMessageDialog(this, message, "VoteNow — Election Completed", JOptionPane.INFORMATION_MESSAGE);
            Election completedElection = new Election(
                    election.electionId,
                    election.title,
                    election.description,
                    election.targetArea,
                    "COMPLETED",
                    election.startTime,
                    LocalDateTime.now()
            );
            dispose();
            new CompletedElectionReportFrame(moderator, completedElection).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error closing election: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeAndReturn() {
        refreshTimer.stop();
        dispose();
        new ModeratorHomeFrame(moderator).setVisible(true);
    }
}