package votenow;

import votenow.Models.VoterSummary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class VoterDirectoryDialog extends JDialog {
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> zoneFilterCombo = new JComboBox<>(new String[]{"All Zones", "North Zone", "South Zone", "East Zone", "West Zone"});
    private final DefaultTableModel tableModel;
    private final JTable voterTable;
    private final JLabel countLabel = new JLabel("0 voters");

    private static final Set<String> VALID_ZONES = Set.of("North Zone", "South Zone", "East Zone", "West Zone");
    private static final Set<String> VALID_GENDERS = Set.of("Male", "Female", "Other");

    public VoterDirectoryDialog(Frame parent) {
        super(parent, "VoteNow — Registered Voter Directory", true);
        setSize(880, 580);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        // Top Bar with Bulk CSV Tools
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton sampleCsvBtn = Theme.createHeaderButton("Save Sample CSV");
        sampleCsvBtn.addActionListener(e -> saveSampleCsv());

        JButton importCsvBtn = Theme.createSuccessButton("Import Voters (CSV)");
        importCsvBtn.addActionListener(e -> importVotersFromCsv());

        topActions.add(sampleCsvBtn);
        topActions.add(importCsvBtn);

        root.add(Theme.createTopBar("Registered Voter Directory", "Search citizen rolls, import bulk CSV records, or manage accounts", topActions), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(Theme.BG_CANVAS);
        body.setBorder(new EmptyBorder(18, 22, 20, 22));

        // Filter Bar
        JPanel filterCard = new JPanel(new BorderLayout(12, 0));
        filterCard.setBackground(Theme.BG_CARD);
        filterCard.setBorder(Theme.cardBorder(Theme.BORDER_LIGHT, 10, 14));

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        Theme.styleTextField(searchField);
        Theme.styleComboBox(zoneFilterCombo);

        JLabel searchLbl = new JLabel("Search Name / Username:");
        searchLbl.setFont(Theme.FONT_BTN);
        searchLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel zoneLbl = new JLabel("Zone:");
        zoneLbl.setFont(Theme.FONT_BTN);
        zoneLbl.setForeground(Theme.TEXT_PRIMARY);

        JButton filterBtn = Theme.createPrimaryButton("Filter");
        filterBtn.addActionListener(e -> loadVoters());

        leftFilters.add(searchLbl);
        leftFilters.add(searchField);
        leftFilters.add(zoneLbl);
        leftFilters.add(zoneFilterCombo);
        leftFilters.add(filterBtn);

        countLabel.setFont(Theme.FONT_BTN);
        countLabel.setForeground(Theme.TEXT_SECONDARY);

        filterCard.add(leftFilters, BorderLayout.WEST);
        filterCard.add(countLabel, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel(new String[]{"Voter ID", "Full Name", "Username", "Residential Zone", "Gender", "Elections Voted"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        voterTable = new JTable(tableModel);
        Theme.styleTable(voterTable);

        JScrollPane scroll = new JScrollPane(voterTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JLabel privacyNote = new JLabel("Ballot secrecy enforced: individual candidate choices are never exposed.");
        privacyNote.setFont(Theme.FONT_SMALL);
        privacyNote.setForeground(Theme.TEXT_SECONDARY);

        JButton deleteBtn = Theme.createDangerButton("Remove Selected Voter");
        deleteBtn.addActionListener(e -> deleteSelectedVoter());

        bottomBar.add(privacyNote, BorderLayout.WEST);
        bottomBar.add(deleteBtn, BorderLayout.EAST);

        body.add(filterCard, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(bottomBar, BorderLayout.SOUTH);

        root.add(body, BorderLayout.CENTER);
        add(root);

        searchField.addActionListener(e -> loadVoters());
        zoneFilterCombo.addActionListener(e -> loadVoters());
        loadVoters();
    }

    private void loadVoters() {
        try {
            List<VoterSummary> voters = VotingDAO.getAllVoters(
                    searchField.getText(),
                    (String) zoneFilterCombo.getSelectedItem()
            );
            tableModel.setRowCount(0);
            for (VoterSummary v : voters) {
                tableModel.addRow(new Object[]{
                        v.userId,
                        v.fullName,
                        v.username,
                        v.area,
                        v.gender,
                        v.electionsVotedCount
                });
            }
            countLabel.setText(voters.size() + " registered voter" + (voters.size() == 1 ? "" : "s"));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading voters: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedVoter() {
        int row = voterTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a voter row first.", "VoteNow — Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove voter '" + name + "' (ID #" + userId + ") from the registry?",
                "VoteNow — Confirm Removal",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            VotingDAO.deleteVoter(userId);
            loadVoters();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting voter: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSampleCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("sample_voters.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("full_name,username,password,area,gender");
            out.println("Neha Sharma,voter11,pass123,North Zone,Female");
            out.println("Arjun Verma,voter12,pass123,East Zone,Male");
            out.println("Tara Menon,voter13,pass123,West Zone,Female");
            JOptionPane.showMessageDialog(this, "Sample CSV saved to:\n" + file.getAbsolutePath(), "VoteNow — Sample Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving sample CSV: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importVotersFromCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Voter CSV File (full_name,username,password,area,gender)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        int imported = 0;
        int skipped = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 5) {
                    skipped++;
                    continue;
                }

                String fullName = parts[0].trim();
                String username = parts[1].trim();
                String password = parts[2].trim();
                String area = parts[3].trim();
                String gender = parts[4].trim();

                // Skip header row
                if (firstLine && ("full_name".equalsIgnoreCase(fullName) || "username".equalsIgnoreCase(username))) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()
                        || !VALID_ZONES.contains(area) || !VALID_GENDERS.contains(gender)) {
                    skipped++;
                    continue;
                }

                if (VotingDAO.registerVoter(fullName, username, password, area, gender)) {
                    imported++;
                } else {
                    skipped++; // Duplicate username
                }
            }

            loadVoters();
            JOptionPane.showMessageDialog(
                    this,
                    String.format("CSV Import Complete:\n• Successfully imported: %d voters\n• Skipped (duplicates/invalid): %d rows", imported, skipped),
                    "VoteNow — Bulk Import Summary",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading CSV: " + ex.getMessage(), "VoteNow — Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}