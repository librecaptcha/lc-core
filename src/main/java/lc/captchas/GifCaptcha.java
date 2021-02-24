package lc.captchas;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import lc.captchas.interfaces.Challenge;
import lc.captchas.interfaces.ChallengeProvider;
import lc.misc.HelperFunctions;
import lc.misc.GifSequenceWriter;

public class GifCaptcha implements ChallengeProvider {

  private BufferedImage charToImg(String text) {
    BufferedImage img = new BufferedImage(250, 100, BufferedImage.TYPE_INT_RGB);
    Font font = new Font("Bradley Hand", Font.ROMAN_BASELINE, 48);
    Graphics2D graphics2D = img.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics2D.setFont(font);
    graphics2D.setColor(new Color((int) (Math.random() * 0x1000000)));
    graphics2D.drawString(text, 45, 45);
    graphics2D.dispose();
    return img;
  }

  private byte[] gifCaptcha(String text) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ImageOutputStream output = new MemoryCacheImageOutputStream(byteArrayOutputStream);
      GifSequenceWriter writer = new GifSequenceWriter(output, 1, 1000, true);
      for (int i = 0; i < text.length(); i++) {
        BufferedImage nextImage = charToImg(String.valueOf(text.charAt(i)));
        writer.writeToSequence(nextImage);
      }
      writer.close();
      output.close();
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Challenge returnChallenge() {
    String secret = HelperFunctions.randomString(6);
    return new Challenge(gifCaptcha(secret), "image/gif", secret.toLowerCase());
  }

  public boolean checkAnswer(String secret, String answer) {
    return answer.toLowerCase().equals(secret);
  }

  public String getId() {
    return "GifCaptcha";
  }
}
