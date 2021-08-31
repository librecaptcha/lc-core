package lc.captchas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.List;
import lc.captchas.interfaces.Challenge;
import lc.captchas.interfaces.ChallengeProvider;
import lc.misc.DPISetter;
import lc.misc.HelperFunctions;

public class FontFunCaptcha implements ChallengeProvider {

  public String getId() {
    return "FontFunCaptcha";
  }

  public Map<String, List<String>> supportedParameters() {
    return Map.of(
        "supportedLevels", List.of("medium"),
        "supportedMedia", List.of("image/png"),
        "supportedInputType", List.of("text"));
  }

  public void configure(String config) {
    // TODO: Add custom config
  }

  private String getFontName(String path, String level) {
    File file = new File(path + level + "/");
    FilenameFilter txtFileFilter =
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            if (name.endsWith(".ttf")) return true;
            else return false;
          }
        };
    File[] files = file.listFiles(txtFileFilter);
    return path
        + level.toLowerCase()
        + "/"
        + files[HelperFunctions.randomNumber(0, files.length - 1)].getName();
  }

  private Font loadCustomFont(String level, String path) {
    String fontName = getFontName(path, level);
    try {
      Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontName));
      font = font.deriveFont(Font.PLAIN, 48f);
      return font;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private byte[] fontFun(String captchaText, String level, String path) {
    String[] colors = {"#f68787", "#f8a978", "#f1eb9a", "#a4f6a5"};
    BufferedImage img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = img.createGraphics();
    for (int i = 0; i < captchaText.length(); i++) {
      Font font = loadCustomFont(level, path);
      graphics2D.setFont(font);
      FontMetrics fontMetrics = graphics2D.getFontMetrics();
      HelperFunctions.setRenderingHints(graphics2D);
      graphics2D.setColor(Color.decode(colors[HelperFunctions.randomNumber(0, 3)]));
      graphics2D.drawString(
          String.valueOf(captchaText.charAt(i)), (i * 48), fontMetrics.getAscent());
    }
    graphics2D.dispose();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      DPISetter dpi = new DPISetter();
      dpi.setDPI(baos, img);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return baos.toByteArray();
  }

  public Challenge returnChallenge() {
    String secret = HelperFunctions.randomString(7);
    String path = "./lib/fonts/";
    return new Challenge(fontFun(secret, "medium", path), "image/png", secret.toLowerCase());
  }

  public boolean checkAnswer(String secret, String answer) {
    return answer.toLowerCase().equals(secret);
  }
}
