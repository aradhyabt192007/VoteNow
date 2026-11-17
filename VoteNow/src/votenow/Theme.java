package votenow;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Theme {
    public static boolean isDarkMode = false;

    // Dynamic Palette (Light Slate by default, switches to Midnight Dark)
    public static Color BG_CANVAS = new Color(241, 245, 249);
    public static Color BG_HEADER = new Color(15, 23, 42);
    public static Color BG_CARD = Color.WHITE;
    public static Color BG_SUBTLE = new Color(248, 250, 252);
    public static Color BG_SELECTED = new Color(239, 246, 255);
    public static Color BG_INPUT = Color.WHITE;

    public static Color TEXT_PRIMARY = new Color(15, 23, 42);
    public static Color TEXT_SECONDARY = new Color(71, 85, 105);
    public static final Color TEXT_ON_DARK = Color.WHITE;

    public static Color PRIMARY = new Color(29, 78, 216);
    public static Color PRIMARY_HOVER = new Color(30, 64, 175);

    public static Color SUCCESS = new Color(4, 120, 87);
    public static Color SUCCESS_HOVER = new Color(6, 95, 70);
    public static Color SUCCESS_BG = new Color(236, 253, 245);

    public static Color WARNING = new Color(180, 83, 9);
    public static Color WARNING_BG = new Color(254, 243, 199);

    public static Color DANGER = new Color(185, 28, 28);
    public static Color DANGER_HOVER = new Color(153, 27, 27);
    public static Color DANGER_BG = new Color(254, 242, 242);

    public static Color SECONDARY_BTN = new Color(226, 232, 240);
    public static Color SECONDARY_HOVER = new Color(203, 213, 225);

    public static Color BORDER_LIGHT = new Color(203, 213, 225);

    public static final Color[] CHART_COLORS = {
            new Color(59, 130, 246),  // Blue
            new Color(16, 185, 129),  // Emerald
            new Color(245, 158, 11),  // Amber
            new Color(139, 92, 246),  // Violet
            new Color(244, 63, 94),   // Rose
            new Color(100, 116, 139)  // Slate
    };

    public static final Font FONT_APP_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_PAGE_TITLE = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_CARD_TITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BTN = new Font("SansSerif", Font.BOLD, 13);

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            BG_CANVAS = new Color(11, 17, 32);         // Deep Navy Canvas (#0B1120)
            BG_HEADER = new Color(15, 23, 42);         // Slate 900 (#0F172A)
            BG_CARD = new Color(30, 41, 59);           // Slate 800 (#1E293B)
            BG_SUBTLE = new Color(23, 33, 49);         // Slate 850
            BG_SELECTED = new Color(30, 58, 138);      // Deep Blue Tint
            BG_INPUT = new Color(15, 23, 42);          // Dark Input

            TEXT_PRIMARY = new Color(241, 245, 249);   // Slate 100
            TEXT_SECONDARY = new Color(148, 163, 184); // Slate 400

            PRIMARY = new Color(59, 130, 246);         // Bright Royal Blue
            PRIMARY_HOVER = new Color(37, 99, 235);

            SUCCESS = new Color(52, 211, 153);         // Bright Emerald
            SUCCESS_HOVER = new Color(16, 185, 129);
            SUCCESS_BG = new Color(6, 78, 59);         // Dark Emerald Tint

            WARNING = new Color(251, 191, 36);         // Bright Amber
            WARNING_BG = new Color(120, 53, 15);       // Dark Amber Tint

            DANGER = new Color(248, 113, 113);         // Soft Coral Red
            DANGER_HOVER = new Color(220, 38, 38);
            DANGER_BG = new Color(127, 29, 29);        // Dark Crimson Tint

            SECONDARY_BTN = new Color(51, 65, 85);     // Slate 700
            SECONDARY_HOVER = new Color(71, 85, 105);  // Slate 600
            BORDER_LIGHT = new Color(51, 65, 85);      // Slate 700
        } else {
            BG_CANVAS = new Color(241, 245, 249);
            BG_HEADER = new Color(15, 23, 42);
            BG_CARD = Color.WHITE;
            BG_SUBTLE = new Color(248, 250, 252);
            BG_SELECTED = new Color(239, 246, 255);
            BG_INPUT = Color.WHITE;

            TEXT_PRIMARY = new Color(15, 23, 42);
            TEXT_SECONDARY = new Color(71, 85, 105);

            PRIMARY = new Color(29, 78, 216);
            PRIMARY_HOVER = new Color(30, 64, 175);

            SUCCESS = new Color(4, 120, 87);
            SUCCESS_HOVER = new Color(6, 95, 70);
            SUCCESS_BG = new Color(236, 253, 245);

            WARNING = new Color(180, 83, 9);
            WARNING_BG = new Color(254, 243, 199);

            DANGER = new Color(185, 28, 28);
            DANGER_HOVER = new Color(153, 27, 27);
            DANGER_BG = new Color(254, 242, 242);

            SECONDARY_BTN = new Color(226, 232, 240);
            SECONDARY_HOVER = new Color(203, 213, 225);
            BORDER_LIGHT = new Color(203, 213, 225);
        }
    }

    public static String getThemeToggleLabel() {
        return isDarkMode ? "Light Mode" : "Dark Mode";
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                new EmptyBorder(16, 20, 16, 20)
        );
    }

    public static Border cardBorder(Color lineColor, int paddingV, int paddingH) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, 1),
                new EmptyBorder(paddingV, paddingH, paddingV, paddingH)
        );
    }

    public static JButton createPrimaryButton(String text) {
        return buildButton(text, PRIMARY, PRIMARY_HOVER, TEXT_ON_DARK);
    }

    public static JButton createSuccessButton(String text) {
        return buildButton(text, isDarkMode ? new Color(5, 150, 105) : SUCCESS, SUCCESS_HOVER, TEXT_ON_DARK);
    }

    public static JButton createDangerButton(String text) {
        return buildButton(text, isDarkMode ? new Color(220, 38, 38) : DANGER, DANGER_HOVER, TEXT_ON_DARK);
    }

    public static JButton createSecondaryButton(String text) {
        return buildButton(text, SECONDARY_BTN, SECONDARY_HOVER, TEXT_PRIMARY);
    }

    public static JButton createHeaderButton(String text) {
        return buildButton(text, new Color(30, 41, 59), new Color(51, 65, 85), TEXT_ON_DARK);
    }

    private static JButton buildButton(String text, Color bg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 15, 9, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bg);
                }
            }
        });
        return btn;
    }

    public static void styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_INPUT);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT),
                new EmptyBorder(7, 10, 7, 10)
        ));
    }

    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
    }

    public static JPanel createTopBar(String titleText, String subtitleText, JComponent rightComponent) {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setBackground(BG_HEADER);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, isDarkMode ? BORDER_LIGHT : BG_HEADER),
                new EmptyBorder(15, 24, 15, 24)
        ));

        JPanel left = new JPanel(new GridLayout(subtitleText == null ? 1 : 2, 1, 0, 3));
        left.setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_ON_DARK);
        left.add(title);

        if (subtitleText != null) {
            JLabel sub = new JLabel(subtitleText);
            sub.setFont(FONT_SMALL);
            sub.setForeground(new Color(148, 163, 184));
            left.add(sub);
        }

        bar.add(left, BorderLayout.WEST);
        if (rightComponent != null) {
            JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rightWrap.setOpaque(false);
            rightWrap.add(rightComponent);
            bar.add(rightWrap, BorderLayout.EAST);
        }
        return bar;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_CARD);
        table.setGridColor(BORDER_LIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(BG_SELECTED);
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                lbl.setBackground(new Color(30, 41, 59));
                lbl.setForeground(TEXT_ON_DARK);
                lbl.setFont(FONT_BTN);
                lbl.setOpaque(true);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                return lbl;
            }
        });

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                lbl.setBorder(new EmptyBorder(0, 14, 0, 14));
                lbl.setFont(FONT_BODY);

                Object lastColVal = tbl.getValueAt(row, tbl.getColumnCount() - 1);
                boolean isHighlightedRow = lastColVal != null && lastColVal.toString().startsWith("Most Voted");

                if (isSelected) {
                    lbl.setBackground(BG_SELECTED);
                    lbl.setForeground(TEXT_PRIMARY);
                } else if (isHighlightedRow) {
                    lbl.setBackground(SUCCESS_BG);
                    lbl.setForeground(SUCCESS);
                    if (column == tbl.getColumnCount() - 1) {
                        lbl.setFont(FONT_BTN);
                    }
                } else {
                    lbl.setBackground(row % 2 == 0 ? BG_CARD : BG_SUBTLE);
                    lbl.setForeground(TEXT_PRIMARY);
                }
                return lbl;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    public static JScrollPane createCleanScrollPane(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CANVAS);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
}