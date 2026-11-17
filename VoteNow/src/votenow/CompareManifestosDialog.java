package votenow;

import votenow.Models.Candidate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CompareManifestosDialog extends JDialog {
    private final List<Candidate> candidates;
    private final JComboBox<String> leftCombo;
    private final JComboBox<String> rightCombo;

    private final JLabel leftNameLbl = new JLabel();
    private final JLabel leftPartyLbl = new JLabel();
    private final JTextArea leftManifestoArea = new JTextArea();

    private final JLabel rightNameLbl = new JLabel();
    private final JLabel rightPartyLbl = new JLabel();
    private final JTextArea rightManifestoArea = new JTextArea();

    public CompareManifestosDialog(Frame parent, String electionTitle, List<Candidate> allCandidates) {
        super(parent, "VoteNow — Compare Candidate Manifestos", true);
        this.candidates = new ArrayList<>();
        for (Candidate c : allCandidates) {
            if (!"NOTA".equalsIgnoreCase(c.candidateName)) {
                this.candidates.add(c);
            }
        }

        setSize(820, 520);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_CANVAS);
        root.add(Theme.createTopBar("Compare Candidate Platforms", electionTitle + " — Side-by-Side Manifesto View", null), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 18, 0));
        body.setBackground(Theme.BG_CANVAS);
        body.setBorder(new EmptyBorder(20, 24, 24, 24));

        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).candidateName + " (" + candidates.get(i).partyName + ")";
        }

        leftCombo = new JComboBox<>(names);
        rightCombo = new JComboBox<>(names);
        Theme.styleComboBox(leftCombo);
        Theme.styleComboBox(rightCombo);

        if (candidates.size() > 1) {
            rightCombo.setSelectedIndex(1);
        }

        body.add(buildCompareColumn("CANDIDATE A", leftCombo, leftNameLbl, leftPartyLbl, leftManifestoArea));
        body.add(buildCompareColumn("CANDIDATE B", rightCombo, rightNameLbl, rightPartyLbl, rightManifestoArea));

        leftCombo.addActionListener(e -> refreshPanels());
        rightCombo.addActionListener(e -> refreshPanels());
        refreshPanels();

        root.add(body, BorderLayout.CENTER);
        add(root);
    }

    private JPanel buildCompareColumn(String headerText, JComboBox<String> combo, JLabel nameLbl, JLabel partyLbl, JTextArea area) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(Theme.cardBorder());

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);

        JLabel tag = new JLabel(headerText);
        tag.setFont(Theme.FONT_SMALL);
        tag.setForeground(Theme.PRIMARY);

        top.add(tag, BorderLayout.NORTH);
        top.add(combo, BorderLayout.CENTER);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setBackground(Theme.BG_SUBTLE);
        titleBox.setBorder(Theme.cardBorder(Theme.BORDER_LIGHT, 10, 12));

        nameLbl.setFont(Theme.FONT_CARD_TITLE);
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        partyLbl.setFont(Theme.FONT_SMALL);
        partyLbl.setForeground(Theme.TEXT_SECONDARY);

        titleBox.add(nameLbl);
        titleBox.add(partyLbl);

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.FONT_BODY);
        area.setBackground(Theme.BG_CARD);
        area.setForeground(Theme.TEXT_PRIMARY);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        scroll.getViewport().setBackground(Theme.BG_CARD);

        JPanel centerWrap = new JPanel(new BorderLayout(0, 10));
        centerWrap.setOpaque(false);
        centerWrap.add(titleBox, BorderLayout.NORTH);
        centerWrap.add(scroll, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(centerWrap, BorderLayout.CENTER);
        return card;
    }

    private void refreshPanels() {
        if (candidates.isEmpty()) return;
        int idxA = Math.max(0, leftCombo.getSelectedIndex());
        int idxB = Math.max(0, rightCombo.getSelectedIndex());

        Candidate a = candidates.get(idxA);
        leftNameLbl.setText(a.candidateName);
        leftPartyLbl.setText("Party: " + a.partyName);
        leftManifestoArea.setText(a.manifesto != null ? a.manifesto : "No manifesto provided.");
        leftManifestoArea.setCaretPosition(0);

        Candidate b = candidates.get(idxB);
        rightNameLbl.setText(b.candidateName);
        rightPartyLbl.setText("Party: " + b.partyName);
        rightManifestoArea.setText(b.manifesto != null ? b.manifesto : "No manifesto provided.");
        rightManifestoArea.setCaretPosition(0);
    }
}