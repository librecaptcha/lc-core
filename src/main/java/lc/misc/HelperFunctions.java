package lc.misc;

import java.awt.*;
import java.util.Random;

public class HelperFunctions {

  private static Random random = new Random();

  public static synchronized void setSeed(long seed) {
    random.setSeed(seed);
  }

  public static int[] parseSize2D(final String size) {
    final String[] fields = size.split("x");
    final int[] result = {Integer.parseInt(fields[0]), Integer.parseInt(fields[1])};
    return result;
  }

  public static void setRenderingHints(Graphics2D g2d) {
    g2d.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  }

  public static final String safeAlphabets = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
  public static final String allAlphabets = safeAlphabets + "ILlO";
  public static final String safeNumbers = "23456789";
  public static final String allNumbers = safeNumbers + "10";
  public static final String specialCharacters = "$#%@&?";
  public static final String safeAlphaNum = safeAlphabets + safeNumbers;
  public static final String safeCharacters = safeAlphaNum + specialCharacters;

  public static String randomString(final int n) {
    return randomString(n, safeCharacters);
  }

  public static String randomString(final int n, final String characters) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < n; i++) {
      int index = randomNumber(characters.length());
      stringBuilder.append(characters.charAt(index));
    }
    return stringBuilder.toString();
  }

  public static synchronized int randomNumber(int min, int max) {
    return random.nextInt((max - min) + 1) + min;
  }

  public static synchronized int randomNumber(int bound) {
    return random.nextInt(bound);
  }
}
