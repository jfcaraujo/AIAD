package com.aiad2021.view;

import javax.swing.*;

public class AuctionGUI extends JFrame{
    private JPanel panel1;
    private JScrollPane scroll;
    private String auctionName;
    private JTextArea textArea2;

    public AuctionGUI(String auctionName){
        textArea2 = new JTextArea(500,500);
        textArea2.setText("");
        textArea2.setLineWrap(true);

        this.auctionName = auctionName;

        add(panel1);
        panel1.setSize(515,560);
        setTitle(auctionName);
        setSize(500,560);
        scroll.setViewportView(textArea2);

    }

    public void addText(String text){
        textArea2.setText(textArea2.getText() + "\n"+ text);
    }
}
