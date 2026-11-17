package votenow;

import votenow.Models.Candidate;
import votenow.Models.CandidateInput;
import votenow.Models.Election;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateElectionDialog extends JDialog {
    private final Election editingElection; // null when creating a new election

    private final JTextField titleField = new JTextField();
    private final JTextField descField = new JTextField();
    private final JComboBox<String> zoneCombo = new JComboBox<>(new String[]{"ALL", "North Zone", "South Zone", "East Zone", "West Zone"});
    private final JSpinner startDelaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1440, 1));
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 72, 1));
    private final JSpinner minutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JTextField candNameField = new JTextField();
    private final JTextField partyNameField = new JTextField();
    private final JTextField manifestoField = new JTextField();

    private final DefaultTableModel candTableModel;
    private final List<CandidateInput> candidateList = new ArrayList<>();
    private boolean saved = false;

    public CreateElectionDialog(Frame parent) {
        this(parent, null);
    }

    public CreateElectionDialog(Frame parent, Election existingElection) {
        super(parent, existingElection == null ? "VoteNow — Create New Election" : "VoteNow — Edit Scheduled Election", true);
        this.editingElection = existingElection;

        setSize(800, 690);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        String headerTitle = editingElection == null ? "Create New Election" : "Edit Scheduled Election";
        String headerSub = "Configure target zone, schedule, & candidates (NOTA is included automatically)";
        root.add(Theme.createTopBar(headerTitle, headerSub, null), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(Theme.BG_CANVAS);
        body.setBorder(new EmptyBorder(18, 22, 18, 22));

        // 1. Election Details Card
        JPanel detailsCard = new JPanel(new GridBagLayout());
        detailsCard.setBackground(Theme.BG_CARD);
        detailsCard.setBorder(Theme.cardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Theme.styleTextField(titleField);
        Theme.styleTextField(descField);
        Theme.styleComboBox(zoneCombo);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        detailsCard.add(makeLabel("Election Title"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        detailsCard.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25;
        detailsCard.add(makeLabel("Description"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        detailsCard.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25;
        detailsCard.add(makeLabel("Eligible Voter Zone"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        detailsCard.add(zoneCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.25;
        detailsCard.add(makeLabel("Schedule & Duration"), gbc);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        timePanel.setOpaque(false);

        JLabel startLbl = makeLabel("Start in (mins, 0 = Now):");
        JLabel durLbl = makeLabel("Duration:");
        JLabel hrsLbl = makeLabel("Hrs");
        JLabel minsLbl = makeLabel("Mins");

        timePanel.add(startLbl);
        timePanel.add(startDelaySpinner);
        timePanel.add(Box.createRigidArea(new Dimension(12, 0)));
        timePanel.add(durLbl);
        timePanel.add(hoursSpinner);
        timePanel.add(hrsLbl);
        timePanel.add(minutesSpinner);
        timePanel.add(minsLbl);

        gbc.gridx = 1; gbc.weightx = 0.75;
        detailsCard.add(timePanel, gbc);

        // 2. Candidate Builder Card
        JPanel candCard = new JPanel(new BorderLayout(0, 10));
        candCard.setBackground(Theme.BG_CARD);
        candCard.setBorder(Theme.cardBorder());

        JPanel addRow = new JPanel(new GridBagLayout());
        addRow.setOpaque(false);
        GridBagConstraints cg = new GridBagConstraints();
        cg.insets = new Insets(4, 4, 4, 4);
        cg.fill = GridBagConstraints.HORIZONTAL;

        Theme.styleTextField(candNameField);
        Theme.styleTextField(partyNameField);
        Theme.styleTextField(manifestoField);

        cg.gridx = 0; cg.gridy = 0; cg.weightx = 0.3;
        addRow.add(makeLabel("Candidate Name"), cg);
        cg.gridx = 1; cg.weightx = 0.3;
        addRow.add(makeLabel("Party Name"), cg);
        cg.gridx = 2; cg.weightx = 0.4;
        addRow.add(makeLabel("Key Manifesto / Promises"), cg);

        cg.gridx = 0; cg.gridy = 1;
        addRow.add(candNameField, cg);
        cg.gridx = 1;
        addRow.add(partyNameField, cg);
        cg.gridx = 2;
        addRow.add(manifestoField, cg);

        JPanel candBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        candBtns.setOpaque(false);
        JButton addCandBtn = Theme.createSecondaryButton("+ Add Candidate");
        JButton removeCandBtn = Theme.createSecondaryButton("Remove Selected");
        candBtns.add(removeCandBtn);
        candBtns.add(addCandBtn);

        JPanel topCandWrap = new JPanel(new BorderLayout(0, 6));
        topCandWrap.setOpaque(false);
        topCandWrap.add(addRow, BorderLayout.CENTER);
        topCandWrap.add(candBtns, BorderLayout.SOUTH);

        candTableModel = new DefaultTableModel(new String[]{"Candidate", "Party", "Manifesto"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable candTable = new JTable(candTableModel);
        Theme.styleTable(candTable);

        addCandBtn.addActionListener(e -> {
            String name = candNameField.getText().trim();
            String party = partyNameField.getText().trim();
            String manifesto = manifestoField.getText().trim();
            if (name.isEmpty() || party.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter both Candidate Name and Party Name.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            candidateList.add(new CandidateInput(name, party, manifesto));
            candTableModel.addRow(new Object[]{name, party, manifesto.isEmpty() ? "—" : manifesto});
            candNameField.setText("");
            partyNameField.setText("");
            manifestoField.setText("");
        });

        removeCandBtn.addActionListener(e -> {
            int row = candTable.getSelectedRow();
            if (row >= 0) {
                candidateList.remove(row);
                candTableModel.removeRow(row);
            }
        });

        JScrollPane scroll = new JScrollPane(candTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        candCard.add(topCandWrap, BorderLayout.NORTH);
        candCard.add(scroll, BorderLayout.CENTER);

        body.add(detailsCard, BorderLayout.NORTH);
        body.add(candCard, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Theme.BG_CARD);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(12, 22, 12, 22)
        ));

        JLabel notaHint = new JLabel("Set 'Start in' > 0 to schedule an Upcoming poll, or 0 to launch live immediately.");
        notaHint.setFont(Theme.FONT_SMALL);
        notaHint.setForeground(Theme.TEXT_SECONDARY);

        JButton saveBtn = Theme.createSuccessButton(editingElection == null ? "Save & Publish Election" : "Save Changes");
        saveBtn.addActionListener(e -> saveElection());

        bottom.add(notaHint, BorderLayout.WEST);
        bottom.add(saveBtn, BorderLayout.EAST);

        root.add(body, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        if (editingElection != null) {
            populateExistingData();
        }

        add(root);
    }

    private void populateExistingData() {
        titleField.setText(editingElection.title);
        descField.setText(editingElection.description);
        zoneCombo.setSelectedItem(editingElection.targetArea);

        long delayMins = Math.max(1, Duration.between(LocalDateTime.now(), editingElection.startTime).toMinutes());
        long durMins = Math.max(1, Duration.between(editingElection.startTime, editingElection.endTime).toMinutes());

        startDelaySpinner.setValue((int) Math.min(1440, delayMins));
        hoursSpinner.setValue((int) (durMins / 60));
        minutesSpinner.setValue((int) (durMins % 60));

        try {
            List<Candidate> existingCands = VotingDAO.getCandidatesForElection(editingElection.electionId);
            for (Candidate c : existingCands) {
                if ("NOTA".equalsIgnoreCase(c.candidateName)) continue;
                candidateList.add(new CandidateInput(c.candidateName, c.partyName, c.manifesto));
                candTableModel.addRow(new Object[]{c.candidateName, c.partyName, c.manifesto});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading candidates: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BTN);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }

    private void saveElection() {
        String title = titleField.getText().trim();
        String desc = descField.getText().trim();
        String targetArea = (String) zoneCombo.getSelectedItem();
        int startDelay = (int) startDelaySpinner.getValue();
        int totalMinutes = ((int) hoursSpinner.getValue()) * 60 + ((int) minutesSpinner.getValue());

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an Election Title.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (totalMinutes <= 0) {
            JOptionPane.showMessageDialog(this, "Poll duration must be at least 1 minute.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (candidateList.size() < 2) {
            JOptionPane.showMessageDialog(this, "Please add at least 2 candidates before saving.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String finalDesc = desc.isEmpty() ? title + " — Official Poll" : desc;

        try {
            if (editingElection == null) {
                VotingDAO.createElectionWithCandidates(title, finalDesc, targetArea, startDelay, totalMinutes, candidateList);
                JOptionPane.showMessageDialog(
                        this,
                        startDelay > 0 ? "Election '" + title + "' scheduled to open in " + startDelay + " minute(s)!" : "Election '" + title + "' is now LIVE!",
                        "VoteNow — Published",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                VotingDAO.updateUpcomingElection(editingElection.electionId, title, finalDesc, targetArea, startDelay, totalMinutes, candidateList);
                JOptionPane.showMessageDialog(this, "Scheduled election updated successfully!", "VoteNow — Updated", JOptionPane.INFORMATION_MESSAGE);
            }
            saved = true;
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}