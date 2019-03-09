package lc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

public class FontFunCaptcha implements ChallengeProvider{

    public String getId() {
        return "FontFunCaptcha";
    }

    private byte[] fontFun(String captchaText){
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
            HelperFunctions.setRenderingHints(graphics2D);
            graphics2D.setColor(Color.decode(colors[rand.nextInt(4)]));
            graphics2D.drawString(String.valueOf(captchaText.toLowerCase().charAt(i)), (i * 48), fontMetrics.getAscent());
        }
        graphics2D.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img,"png",baos);
        }catch (Exception e){
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public Challenge returnChallenge() {
        String secret = HelperFunctions.randomString(7);
        return new Challenge(fontFun(secret),"png",secret);
    }

    public boolean checkAnswer(String secret, String answer){
        return secret.equals(answer);
    }
}
