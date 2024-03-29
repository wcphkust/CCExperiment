package org.apache.batik.svggen;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Font;
public class Bug6535 implements Painter {
    public void paint(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(Color.black);
        g.scale(10,10);
        Font font=new Font("Arial", Font.PLAIN, 1);
        Font font2=font.deriveFont(1.5f);
        g.setFont(font);
        g.drawString("Hello, size 10", 4, 4);
        g.setFont(font2);
        g.drawString("Hello, size 15", 4, 8);
        g.scale(.1, .1);
        font=new Font("Arial", Font.PLAIN, 10);
        font2=font.deriveFont(15f);
        g.setFont(font);
        g.drawString("Hello, size 10", 160, 40);
        g.setFont(font2);
        g.drawString("Hello, size 15", 160, 80);
    }
}
