package lc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.stream.Stream;

public class FontFunCaptcha implements ChallengeProvider{

    public String getId() {
        return "FontFunCaptcha";
    }

    private int noOfFiles(String path,String level){
        try(Stream<Path> files = Files.list(Paths.get(path+level))){
            return (short)files.count()-1;
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private Font loadCustomFont(String level, String path){
        String fontName = path+level.toLowerCase()+"/font"+HelperFunctions.randomNumber(1,noOfFiles(path,level))+".ttf";
        try{
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontName));
            font = font.deriveFont(Font.PLAIN, 48f);
            return font;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private byte[] fontFun(String captchaText, String level, String path){
        String[] colors = {"#f68787","#f8a978","#f1eb9a","#a4f6a5"};
        BufferedImage img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = img.createGraphics();
        for(int i=0; i< captchaText.length(); i++) {
            Font font = loadCustomFont(level,path);
            graphics2D.setFont(font);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            HelperFunctions.setRenderingHints(graphics2D);
            graphics2D.setColor(Color.decode(colors[HelperFunctions.randomNumber(0,3)]));
            graphics2D.drawString(String.valueOf(captchaText.charAt(i)), (i * 48), fontMetrics.getAscent());
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
        String path = "./lib/fonts/";
        return new Challenge(fontFun(secret,"medium",path),"png",secret.toLowerCase());
    }

    public boolean checkAnswer(String secret, String answer){
        return answer.toLowerCase().equals(secret);
    }
}
