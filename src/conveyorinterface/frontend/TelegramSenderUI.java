// =================================================================================
// Base                 : Conveyor Sortation Controller
// Class                : TelegramSenderUI
// Programmer           : Giresh
// Release Date         : 2025-11-10
// Description          : Industrial-level Swing frontend with 8 telegram buttons
// =================================================================================

package conveyorinterface.frontend;

import conveyorinterface.plctelegrams.TelegramDispatcher;
import platform.core.log.Log;

import javax.swing.*;
import java.awt.*;

public class TelegramSenderUI {

    private final TelegramDispatcher dispatcher;

    public TelegramSenderUI(TelegramDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void showUI() {
        JFrame frame = new JFrame("Conveyor Sortation Controller");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 400);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 245, 245)); // light grey

        // --- Title Label ---
        JLabel title = new JLabel("Conveyor Sortation Controller", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        frame.add(title, BorderLayout.NORTH);

        // --- Panel for Buttons ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(frame.getContentPane().getBackground());
        buttonPanel.setLayout(new GridLayout(4, 2, 15, 15)); // 4 rows x 2 cols
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 13);

        // --- Buttons ---
        JButton btn1 = createButton("Send Airline Table Start", buttonFont);
        btn1.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendAirlineCodeTableStart(1), 
                "Airline Table Start sent successfully!"));

        JButton btn2 = createButton("Send Airline Code Entry", buttonFont);
      //  btn2.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendAirlineCodeTableEntry(1),
     //           "Airline Code Entry sent successfully!"));

        JButton btn3 = createButton("Send Airline Table End", buttonFont);
        btn3.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendAirlineCodeTableEnd(1),
                "Airline Table End sent successfully!"));

        JButton btn4 = createButton("Send Fallback Tag Table Start", buttonFont);
        btn4.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendFallBackTableStart(1), 
        		"Fallback Tag Table Start sent successfully!"));

        JButton btn5 = createButton("Send Fallback Tag Entry", buttonFont);
        btn5.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendFallbackTagEntryTlg(1),
        		"Fallback Tag Entry sent successfully!"));

        JButton btn6 = createButton("Send Fallback Tag Table End", buttonFont);
        btn6.addActionListener(e -> sendTelegram(frame, () -> dispatcher.sendFallBackTableEnd(1), 
        		"Fallback Tag Table End sent successfully!"));

        JButton btn7 = createButton("Send Item Info Request", buttonFont);
        btn7.addActionListener(e -> sendTelegram(frame, () -> {}, "Item Info Request sent successfully!"));

        JButton btn8 = createButton("Send Status Update", buttonFont);
        btn8.addActionListener(e -> sendTelegram(frame, () -> {}, "Status Update sent successfully!"));

        // --- Add buttons to panel ---
        buttonPanel.add(btn1);
        buttonPanel.add(btn2);
        buttonPanel.add(btn3);
        buttonPanel.add(btn4);
        buttonPanel.add(btn5);
        buttonPanel.add(btn6);
        buttonPanel.add(btn7);
        buttonPanel.add(btn8);

        frame.add(buttonPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- Button Factory ---
    private JButton createButton(String text, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setBackground(new Color(70, 130, 180)); // steel blue
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // --- Telegram sender helper ---
    private void sendTelegram(JFrame frame, TelegramAction action, String successMsg) {
        try {
            action.send();
            JOptionPane.showMessageDialog(frame, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to send telegram: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    private interface TelegramAction {
        void send() throws Exception;
    }
}
