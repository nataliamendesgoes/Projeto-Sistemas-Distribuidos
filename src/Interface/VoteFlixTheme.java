package Interface;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class VoteFlixTheme {
    

    public static final Color BG_DARK = new Color(0, 0, 0);           
    public static final Color SIDEBAR_BG = new Color(165, 40, 0);     
    public static final Color ORANGE_VOTEFLIX = new Color(220, 70, 0);
    public static final Color CONTENT_BG = Color.WHITE;               
    public static final Color CARD_BG = new Color(225, 225, 225);     
    public static final Color INPUT_BG = new Color(220, 220, 220);   
    public static final Font FONT_LOGO = new Font("Arial", Font.BOLD, 32);
    public static final Font FONT_NAV = new Font("Arial", Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 40);
    public static final Font FONT_LABEL = new Font("Arial", Font.BOLD, 16);
    public static final Font FONT_INPUT = new Font("Arial", Font.PLAIN, 18);
    public static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 16);

    
    public static void styleNavButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_NAV);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

   
    public static void styleSidebarButton(JButton btn) {
        btn.setBackground(SIDEBAR_BG);
        btn.setForeground(Color.BLACK); 
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false); 
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT); 
        btn.setBorder(new EmptyBorder(10, 20, 10, 10)); 
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }


    public static void stylePrimaryButton(JButton btn) {
        btn.setBackground(ORANGE_VOTEFLIX);
        btn.setForeground(Color.BLACK); 
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorder(new LineBorder(ORANGE_VOTEFLIX, 1, true));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static void styleTextField(JTextField txt) {
        txt.setBackground(INPUT_BG);
        txt.setForeground(Color.BLACK);
        txt.setFont(FONT_INPUT);
        txt.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.GRAY, 1), 
            new EmptyBorder(5, 10, 5, 10)
        ));
    }
    
    public static void styleLabel(JLabel lbl) {
        lbl.setForeground(Color.BLACK);
        lbl.setFont(FONT_LABEL);
    }
}