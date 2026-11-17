package votenow;

import votenow.Models.Candidate;
import votenow.Models.Election;
import votenow.Models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingFrame extends JFrame {
    private final User voter;
    private final Election election;
    private final ButtonGroup candidateGroup = new ButtonGroup();
    private final Map<JRadioButton, Candidate> radioCandidateMap = new HashMap<>();
    private final List<JPanel> candidateRows = new ArrayList<>();
    private final List<JRadioButton> radioButtons = new ArrayList<>();
    private List<Candidate> loadedCandidates = new ArrayList<>();

    public VotingFrame(User voter, Election election) {
        this.voter = voter;
        this.election = election;

        setTitle("VoteNow — " + election.title);
        setSize(780, 580);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        // Top Bar Actions
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton compareBtn = Theme.createPrimaryButton("Compare Candidates");
        compareBtn.addActionListener(e -> new CompareManifestosDialog(this, election.title, loadedCandidates).setVisible(true));

        JButton backBtn = Theme.createHeaderButton("<- Back");
        backBtn.addActionListener(e -> {
            dispose();
            new VoterHomeFrame(voter).setVisible(true);
        });

        topActions.add(compareBtn);
        topActions.add(backBtn);

        root.add(Theme.createTopBar(election.title, "Official Electronic Ballot  •  Compare platforms & select one option", topActions), BorderLayout.NORTH);

        JPanel candidatesPanel = new JPanel();
        candidatesPanel.setLayout(new BoxLayout(candidatesPanel, BoxLayout.Y_AXIS));
        candidatesPanel.setBackground(Theme.BG_CANVAS);
        candidatesPanel.setBorder(new EmptyBorder(20, 24, 12, 24));

        try {
            loadedCandidates = VotingDAO.getCandidatesForElection(election.electionId);
            for (Candidate c : loadedCandidates) {
                JPanel row = createCandidateRow(c);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                candidatesPanel.add(row);
                candidatesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading candidates: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }

        root.add(Theme.createCleanScrollPane(candidatesPanel), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.BG_CARD);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(14, 24, 14, 24)
        ));

        JLabel note = new JLabel("You will receive a unique confirmation receipt upon casting your ballot.");
        note.setFont(Theme.FONT_SMALL);
        note.setForeground(Theme.TEXT_SECONDARY);

        JButton submitBtn = Theme.createSuccessButton("Confirm & Submit Vote");
        submitBtn.addActionListener(e -> submitVote());

        bottomBar.add(note, BorderLayout.WEST);
        bottomBar.add(submitBtn, BorderLayout.EAST);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createCandidateRow(Candidate c) {
        boolean isNota = c.candidateName.equalsIgnoreCase("NOTA");

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Theme.BG_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        row.setBorder(Theme.cardBorder(Theme.BORDER_LIGHT, 12, 16));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JRadioButton rb = new JRadioButton();
        rb.setOpaque(false);
        rb.setFocusPainted(false);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel nameLbl = new JLabel(isNota ? "NOTA (None of the Above)" : c.candidateName);
        nameLbl.setFont(Theme.FONT_CARD_TITLE);
        nameLbl.setForeground(isNota ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY);

        JLabel partyLbl = new JLabel(isNota ? "Decline to vote for any listed candidate" : "Party: " + c.partyName);
        partyLbl.setFont(Theme.FONT_SMALL);
        partyLbl.setForeground(Theme.TEXT_SECONDARY);

        infoPanel.add(nameLbl);
        infoPanel.add(partyLbl);

        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setOpaque(false);
        JButton manifestoBtn = Theme.createSecondaryButton("Manifesto");
        manifestoBtn.addActionListener(e -> showManifestoDialog(c));
        rightWrap.add(manifestoBtn);

        candidateGroup.add(rb);
        radioCandidateMap.put(rb, c);
        candidateRows.add(row);
        radioButtons.add(rb);

        Runnable selectAction = () -> {
            rb.setSelected(true);
            refreshSelectionHighlight();
        };

        rb.addActionListener(e -> refreshSelectionHighlight());
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAction.run();
            }
        });

        row.add(rb, BorderLayout.WEST);
        row.add(infoPanel, BorderLayout.CENTER);
        row.add(rightWrap, BorderLayout.EAST);
        return row;
    }

    private void showManifestoDialog(Candidate c) {
        JTextArea area = new JTextArea(c.manifesto != null ? c.manifesto : "No manifesto details provided.");
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.FONT_BODY);
        area.setBackground(Theme.BG_SUBTLE);
        area.setForeground(Theme.TEXT_PRIMARY);
        area.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(430, 180));
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));

        JOptionPane.showMessageDialog(
                this,
                scroll,
                c.candidateName + " (" + c.partyName + ") — Manifesto",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void refreshSelectionHighlight() {
        for (int i = 0; i < radioButtons.size(); i++) {
            boolean selected = radioButtons.get(i).isSelected();
            JPanel row = candidateRows.get(i);
            row.setBackground(selected ? Theme.BG_SELECTED : Theme.BG_CARD);
            row.setBorder(Theme.cardBorder(selected ? Theme.PRIMARY : Theme.BORDER_LIGHT, 12, 16));
        }
    }

    private void submitVote() {
        Candidate selected = null;
        for (Map.Entry<JRadioButton, Candidate> entry : radioCandidateMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                selected = entry.getValue();
                break;
            }
        }

        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a candidate or NOTA before submitting.", "VoteNow — No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cast your vote for " + selected.candidateName + " (" + selected.partyName + ")?\nThis cannot be undone.",
                "VoteNow — Confirm Ballot",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String receiptCode = VotingDAO.castVote(election.electionId, voter.userId, selected.candidateId);
            if (receiptCode != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Your vote has been securely recorded!\n\nConfirmation Receipt Code: " + receiptCode +
                                "\n(View all your receipts anytime from 'My Vote Receipts' on your dashboard.)",
                        "VoteNow — Ballot Receipt",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(this, "You have already voted in this election.", "VoteNow — Duplicate Vote", JOptionPane.WARNING_MESSAGE);
            }
            dispose();
            new VoterHomeFrame(voter).setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error casting vote: " + ex.getMessage(), "VoteNow — Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}