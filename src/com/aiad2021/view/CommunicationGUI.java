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

    public CommunicationGUI(String name){
        textArea2 = new JTextArea(500,500);
        textArea2.setText("");
        textArea2.setLineWrap(true);

        add(panel);

        panel.setSize(515,560);
        setTitle(name);
        setSize(500,560);
        scroll.setViewportView(textArea2);

        textArea1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER){
                    User.handleMessage(textArea1.getText());
                    textArea2.setText(textArea2.getText() + "\n Command inserted: "+ textArea1.getText());
                    textArea1.setText("");
                }
            }
        });
    }

    public void addText(String text){
        textArea2.setText(textArea2.getText() + "\n"+ text);
    }
}
