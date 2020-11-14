package com.aiad2021.view;

import com.aiad2021.Agents.User;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CommunicationGUI extends JFrame{
    private JTextArea textArea1;
    private JPanel panel;
    private JScrollPane scroll;
    private JTextArea textArea2;

    public CommunicationGUI(User user){
        textArea2 = new JTextArea(500,500);
        textArea2.setText("");
        textArea2.setLineWrap(true);

        add(panel);

        panel.setSize(515,560);
        setTitle(user.getName());
        setSize(500,560);
        scroll.setViewportView(textArea2);

        textArea1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER){
                    textArea2.setText(textArea2.getText() + "\n Command inserted: "+ textArea1.getText());
                    user.handleMessage(textArea1.getText().substring(0,textArea1.getText().length() -1));
                    textArea1.setText("");
                }
            }
        });
    }

    public void addText(String text){
        textArea2.setText(textArea2.getText() + "\n"+ text);
    }
}
