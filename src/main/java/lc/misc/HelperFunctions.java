package lc.misc;

import java.awt.*;

public class HelperFunctions {

  public static void setRenderingHints(Graphics2D g2d) {
    g2d.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  }

  public static String randomString(int n) {
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz23456789$#%@&?";
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < n; i++) {
      int index = (int) (characters.length() * Math.random());
      stringBuilder.append(characters.charAt(index));
    }
    return stringBuilder.toString();
  }

  public static int randomNumber(int min, int max) {
    return (int) (Math.random() * ((max - min) + 1)) + min;
  }
}
