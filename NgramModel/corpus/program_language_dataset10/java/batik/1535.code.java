package org.apache.batik.svggen;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
public class Color1 implements Painter {
    public void paint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.AffineTransform defaultTransform = g.getTransform();
        java.awt.Color labelColor = java.awt.Color.black;
        java.awt.Color[] colorConstants = { java.awt.Color.black,
                                   java.awt.Color.blue,
                                   java.awt.Color.cyan,
                                   java.awt.Color.darkGray,
                                   java.awt.Color.gray,
                                   java.awt.Color.green,
                                   java.awt.Color.lightGray,
                                   java.awt.Color.magenta,
                                   java.awt.Color.orange,
                                   java.awt.Color.pink,
                                   java.awt.Color.red,
                                   java.awt.Color.white,
                                   java.awt.Color.yellow };
        String[] colorConstantStrings =  { "black",
                                           "blue",
                                           "cyan",
                                           "darkGray",
                                           "gray",
                                           "green",
                                           "lightGray",
                                           "magenta",
                                           "orange",
                                           "pink",
                                           "red",
                                           "white",
                                           "yellow" };
        g.translate(20, 20);
        g.setPaint(labelColor);
        g.drawString("Color Constants", -5, 0);
        g.translate(0, 20);
        for(int i=0; i<colorConstants.length; i++){
            g.setPaint(labelColor);
            g.drawString(colorConstantStrings[i], 10, 3);
            g.setPaint(colorConstants[i]);
            g.fillRect(-5, -5, 10, 10);
            g.setPaint(labelColor);
            g.drawRect(-5, -5, 10, 10);
            g.translate(0, 20);
        }
        g.setTransform(defaultTransform);
        g.translate(150, 20);
        g.setColor(labelColor);
        g.drawString("Various opacities", 0, 0);
        g.translate(0, 10);
        int opacitySteps = 20;
        g.setPaint(new java.awt.Color(80, 255, 80));
        g.fillRect(0, 0, 40, 260);
        int stepHeight = 260/opacitySteps;
        Font defaultFont = g.getFont();
        Font opacityFont = new Font(defaultFont.getFamily(),
                                    defaultFont.getStyle(),
                                    (int)(defaultFont.getSize()*0.8));
        g.setFont(opacityFont);
        for(int i=0; i<opacitySteps; i++){
            int opacity = ((i + 1)*255)/opacitySteps;
            java.awt.Color color = new java.awt.Color(0, 0, 0, opacity);
            g.setPaint(color);
            g.fillRect(0, 0, 40, stepHeight);
            g.setPaint(labelColor);
            g.drawString( String.valueOf( opacity ), 50, stepHeight/2);
            g.translate(0, stepHeight);
        }
    }
}