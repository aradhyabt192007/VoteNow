package votenow;

import votenow.Models.Election;
import votenow.Models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ModeratorHomeFrame extends JFrame {
    private final User moderator;

    public ModeratorHomeFrame(User moderator) {
        this.moderator = moderator;
        setTitle("VoteNow — Moderator Console");
        setSize(900, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        // Top Bar Actions
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton createElectionBtn = Theme.createPrimaryButton("+ New Election");
        createElectionBtn.addActionListener(e -> {
            CreateElectionDialog dialog = new CreateElectionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                reloadFrame();
            }
        });

        JButton directoryBtn = Theme.createHeaderButton("Voter Directory");
        directoryBtn.addActionListener(e -> new VoterDirectoryDialog(this).setVisible(true));

        JButton openVoterBtn = Theme.createSuccessButton("+ Voter Window");
        openVoterBtn.addActionListener(e -> new LoginFrame().setVisible(true));

        JButton themeBtn = Theme.createHeaderButton(Theme.getThemeToggleLabel());
        themeBtn.addActionListener(e -> {
            Theme.toggleTheme();
            reloadFrame();
        });

        JButton logoutBtn = Theme.createHeaderButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        topActions.add(createElectionBtn);
        topActions.add(directoryBtn);
        topActions.add(openVoterBtn);
        topActions.add(themeBtn);
        topActions.add(logoutBtn);

        root.add(Theme.createTopBar("VoteNow — Moderator Console", "Signed in as " + moderator.fullName, topActions), BorderLayout.NORTH);

        // Main Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_CANVAS);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        try {
            // 1. In-Progress Elections
            JLabel activeHeader = new JLabel("ELECTIONS IN PROGRESS (LIVE TELEMETRY)");
            activeHeader.setFont(Theme.FONT_SECTION);
            activeHeader.setForeground(Theme.SUCCESS);
            activeHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(activeHeader);
            content.add(Box.createRigidArea(new Dimension(0, 10)));

            List<Election> inProgress = VotingDAO.getElectionsByStatus("IN_PROGRESS");
            if (inProgress.isEmpty()) {
                content.add(makeEmptyLabel("No elections currently in progress."));
            } else {
                for (Election e : inProgress) {
                    JPanel card = createElectionCard(e);
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    content.add(card);
                    content.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }

            content.add(Box.createRigidArea(new Dimension(0, 14)));

            // 2. Upcoming Scheduled Elections
            JLabel upcomingHeader = new JLabel("UPCOMING SCHEDULED ELECTIONS (EDITABLE BEFORE POLL OPENS)");
            upcomingHeader.setFont(Theme.FONT_SECTION);
            upcomingHeader.setForeground(Theme.WARNING);
            upcomingHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(upcomingHeader);
            content.add(Box.createRigidArea(new Dimension(0, 10)));

            List<Election> upcoming = VotingDAO.getElectionsByStatus("UPCOMING");
            if (upcoming.isEmpty()) {
                content.add(makeEmptyLabel("No upcoming scheduled elections."));
            } else {
                for (Election e : upcoming) {
                    JPanel card = createElectionCard(e);
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    content.add(card);
                    content.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }

            content.add(Box.createRigidArea(new Dimension(0, 14)));

            // 3. Completed Elections
            JLabel completedHeader = new JLabel("COMPLETED ELECTIONS (FINAL RESULTS & ANALYTICS)");
            completedHeader.setFont(Theme.FONT_SECTION);
            completedHeader.setForeground(Theme.PRIMARY);
            completedHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(completedHeader);
            content.add(Box.createRigidArea(new Dimension(0, 10)));

            List<Election> completed = VotingDAO.getElectionsByStatus("COMPLETED");
            for (Election e : completed) {
                JPanel card = createElectionCard(e);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                content.add(card);
                content.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }

        root.add(Theme.createCleanScrollPane(content), BorderLayout.CENTER);
        add(root);
    }

    private JLabel makeEmptyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BODY);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void reloadFrame() {
        dispose();
        new ModeratorHomeFrame(moderator).setVisible(true);
    }

    private JPanel createElectionCard(Election election) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(Theme.cardBorder());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 5));
        info.setOpaque(false);

        String zoneTag = "ALL".equalsIgnoreCase(election.targetArea) ? "All Zones" : election.targetArea;
        JLabel title = new JLabel(election.title + "  [" + zoneTag + "]");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        String subText = election.description;
        if ("UPCOMING".equals(election.status)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
            subText = "Opens: " + election.startTime.format(fmt) + "  •  " + election.description;
        }

        JLabel desc = new JLabel(subText);
        desc.setFont(Theme.FONT_BODY);
        desc.setForeground(Theme.TEXT_SECONDARY);

        info.add(title);
        info.add(desc);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        rightWrap.setOpaque(false);

        if ("IN_PROGRESS".equals(election.status)) {
            JButton openBtn = Theme.createSuccessButton("Monitor Live  ->");
            openBtn.addActionListener(e -> {
                dispose();
                new LiveElectionMonitorFrame(moderator, election).setVisible(true);
            });
            rightWrap.add(openBtn);
        } else if ("UPCOMING".equals(election.status)) {
            JButton editBtn = Theme.createSecondaryButton("Edit");
            editBtn.addActionListener(e -> {
                CreateElectionDialog dialog = new CreateElectionDialog(this, election);
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    reloadFrame();
                }
            });

            JButton cancelBtn = Theme.createDangerButton("Cancel Poll");
            cancelBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Cancel and delete scheduled election '" + election.title + "'?",
                        "VoteNow — Cancel Poll",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        VotingDAO.deleteElection(election.electionId);
                        reloadFrame();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error cancelling poll: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            rightWrap.add(editBtn);
            rightWrap.add(cancelBtn);
        } else {
            JButton reportBtn = Theme.createPrimaryButton("View Analytics  ->");
            reportBtn.addActionListener(e -> {
                dispose();
                new CompletedElectionReportFrame(moderator, election).setVisible(true);
            });
            rightWrap.add(reportBtn);
        }

        card.add(info, BorderLayout.CENTER);
        card.add(rightWrap, BorderLayout.EAST);
        return card;
    }
}