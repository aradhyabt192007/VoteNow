package votenow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class RegisterVoterDialog extends JDialog {
    private final JTextField fullNameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<String> areaCombo = new JComboBox<>(new String[]{"North Zone", "South Zone", "East Zone", "West Zone"});
    private final JComboBox<String> genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});

    public RegisterVoterDialog(Frame parent) {
        super(parent, "VoteNow — New Voter Registration", true);
        setSize(460, 470);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);
        root.add(Theme.createTopBar("Voter Registration", "Create your VoteNow citizen account", null), BorderLayout.NORTH);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setBackground(Theme.BG_CANVAS);
        formWrap.setBorder(new EmptyBorder(20, 26, 20, 26));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.BG_CARD);
        card.setBorder(Theme.cardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 6, 7, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Theme.styleTextField(fullNameField);
        Theme.styleTextField(usernameField);
        Theme.styleTextField(passwordField);
        Theme.styleComboBox(areaCombo);
        Theme.styleComboBox(genderCombo);

        addRow(card, gbc, 0, "Full Name", fullNameField);
        addRow(card, gbc, 1, "Username", usernameField);
        addRow(card, gbc, 2, "Password", passwordField);
        addRow(card, gbc, 3, "Residential Zone", areaCombo);
        addRow(card, gbc, 4, "Gender", genderCombo);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 4, 6);
        JButton registerBtn = Theme.createSuccessButton("Complete Registration");
        registerBtn.addActionListener(e -> handleRegister());
        card.add(registerBtn, gbc);

        formWrap.add(card, BorderLayout.CENTER);
        root.add(formWrap, BorderLayout.CENTER);
        add(root);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_BTN);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        panel.add(field, gbc);
    }

    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String area = (String) areaCombo.getSelectedItem();
        String gender = (String) genderCombo.getSelectedItem();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean created = VotingDAO.registerVoter(fullName, username, password, area, gender);
            if (!created) {
                JOptionPane.showMessageDialog(this, "Username '" + username + "' is already taken. Choose another.", "VoteNow — Duplicate Username", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Registration successful! You can now log in as '" + username + "'.", "VoteNow — Registered", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}