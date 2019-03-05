package lc;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.File;
import java.util.Random;
import java.io.ByteArrayOutputStream;

public class CaptchaTypes{

    private String secret = "";

    private void setRenderingHints(Graphics2D g2d){
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private BufferedImage charToImg(String text){
        BufferedImage img = new BufferedImage(250, 100, BufferedImage.TYPE_INT_RGB);
        Font font = new Font("Bradley Hand", Font.ROMAN_BASELINE, 48);
        Graphics2D graphics2D = img.createGraphics();
        graphics2D.setFont(font);
        graphics2D.setColor(new Color((int)(Math.random() * 0x1000000)));
        graphics2D.drawString( text , 45, 45);
        graphics2D.dispose();
        return img;
    }

    public byte[] type_1(String captchaText){
        String[] fonts = {"Captcha Code","Mom'sTypewriter","Annifont","SF Intoxicated Blues",
                "BeachType","Batmos","Barbecue","Bad Seed","Aswell","Alien Marksman"};
        String[] colors = {"#f68787","#f8a978","#f1eb9a","#a4f6a5"};
        BufferedImage img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = img.createGraphics();
        Random rand = new Random();
        for(int i=0; i< captchaText.length(); i++) {
            Font font = new Font(fonts[rand.nextInt(10)], Font.ROMAN_BASELINE, 48);
            graphics2D.setFont(font);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            setRenderingHints(graphics2D);
            graphics2D.setColor(Color.decode(colors[rand.nextInt(4)]));
            if(rand.nextBoolean()) {
                graphics2D.drawString(String.valueOf(captchaText.toLowerCase().charAt(i)), (i * 48), fontMetrics.getAscent());
            }
            else {
                graphics2D.drawString(String.valueOf(captchaText.toUpperCase().charAt(i)), (i * 48), fontMetrics.getAscent());
            }
        }
        graphics2D.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try{
            ImageIO.write(img, "png",new File("Text.png"));
            ImageIO.write(img,"png",baos);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        return baos.toByteArray();
    }

    public void type_2(String text){
        try {
            ImageOutputStream output = new FileImageOutputStream(new File("captchaTest.gif"));
            GifSequenceWriter writer = new GifSequenceWriter( output, 1,1000, true );
            for(int i=0; i< text.length(); i++){
                BufferedImage nextImage = charToImg(String.valueOf(text.charAt(i)));
                writer.writeToSequence(nextImage);
            }
            writer.close();
            output.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void type_3(String text){
        BufferedImage img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB);
        Font font = new Font("Arial",Font.ROMAN_BASELINE ,48);
        Graphics2D graphics2D = img.createGraphics();
        TextLayout textLayout = new TextLayout(text, font, graphics2D.getFontRenderContext());
        setRenderingHints(graphics2D);
        graphics2D.setPaint(Color.WHITE);
        graphics2D.fillRect(0, 0, 350, 100);
        graphics2D.setPaint(Color.BLACK);
        textLayout.draw(graphics2D, 15, 50);
        graphics2D.dispose();
        float[] kernel = {
                1f / 9f, 1f / 9f, 1f / 9f,
                1f / 9f, 1f / 9f, 1f / 9f,
                1f / 9f, 1f / 9f, 1f / 9f
        };
        ConvolveOp op =  new ConvolveOp(new Kernel(3, 3, kernel),
                ConvolveOp.EDGE_NO_OP, null);
        BufferedImage img2 = op.filter(img, null);
        Graphics2D g2d = img2.createGraphics();
        setRenderingHints(g2d);
        g2d.setPaint(Color.WHITE);
        textLayout.draw(g2d, 13, 50);
        g2d.dispose();
        try{
            ImageIO.write(img2, "png",new File("Te.png"));
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }
}

