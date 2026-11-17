package votenow;

import votenow.Models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private final JComboBox<String> roleComboBox;
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public LoginFrame() {
        setTitle("VoteNow — Login");
        setSize(490, 510);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);

        JButton themeBtn = Theme.createHeaderButton(Theme.getThemeToggleLabel());
        themeBtn.addActionListener(e -> {
            Theme.toggleTheme();
            dispose();
            new LoginFrame().setVisible(true);
        });

        root.add(Theme.createTopBar("VoteNow", "Secure Digital Election Portal", themeBtn), BorderLayout.NORTH);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(Theme.BG_CANVAS);
        centerWrap.setBorder(new EmptyBorder(22, 32, 16, 32));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Theme.BG_CARD);
        formCard.setBorder(Theme.cardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel roleLbl = new JLabel("Login As");
        roleLbl.setFont(Theme.FONT_BTN);
        roleLbl.setForeground(Theme.TEXT_PRIMARY);
        formCard.add(roleLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        roleComboBox = new JComboBox<>(new String[]{"VOTER", "MODERATOR"});
        Theme.styleComboBox(roleComboBox);
        roleComboBox.setPreferredSize(new Dimension(200, 36));
        formCard.add(roleComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(Theme.FONT_BTN);
        userLbl.setForeground(Theme.TEXT_PRIMARY);
        formCard.add(userLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        usernameField = new JTextField();
        Theme.styleTextField(usernameField);
        formCard.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(Theme.FONT_BTN);
        passLbl.setForeground(Theme.TEXT_PRIMARY);
        formCard.add(passLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        passwordField = new JPasswordField();
        Theme.styleTextField(passwordField);
        formCard.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 4, 6);
        JButton loginButton = Theme.createPrimaryButton("Login to VoteNow");
        formCard.add(loginButton, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(6, 6, 2, 6);
        JButton registerBtn = Theme.createSecondaryButton("New Voter? Register Account");
        registerBtn.addActionListener(e -> new RegisterVoterDialog(this).setVisible(true));
        formCard.add(registerBtn, gbc);

        centerWrap.add(formCard, BorderLayout.CENTER);

        JPanel hintCard = new JPanel(new GridLayout(2, 1, 2, 4));
        hintCard.setBackground(Theme.BG_SUBTLE);
        hintCard.setBorder(Theme.cardBorder(Theme.BORDER_LIGHT, 10, 14));

        JLabel voterHint = new JLabel("Voter Demo: voter1  /  pass123", SwingConstants.CENTER);
        voterHint.setFont(Theme.FONT_SMALL);
        voterHint.setForeground(Theme.TEXT_SECONDARY);

        JLabel modHint = new JLabel("Moderator Demo: admin  /  admin123", SwingConstants.CENTER);
        modHint.setFont(Theme.FONT_SMALL);
        modHint.setForeground(Theme.TEXT_SECONDARY);

        hintCard.add(voterHint);
        hintCard.add(modHint);

        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.setBackground(Theme.BG_CANVAS);
        southWrap.setBorder(new EmptyBorder(0, 32, 20, 32));
        southWrap.add(hintCard, BorderLayout.CENTER);

        root.add(centerWrap, BorderLayout.CENTER);
        root.add(southWrap, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> performLogin());
        getRootPane().setDefaultButton(loginButton);

        add(root);
    }

    private void performLogin() {
        String role = (String) roleComboBox.getSelectedItem();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "VoteNow — Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = VotingDAO.authenticate(username, password, role);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username, password, or role.", "VoteNow — Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dispose();
            if ("VOTER".equals(user.role)) {
                new VoterHomeFrame(user).setVisible(true);
            } else {
                new ModeratorHomeFrame(user).setVisible(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "VoteNow — Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}