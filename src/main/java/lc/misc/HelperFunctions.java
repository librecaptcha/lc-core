package lc.misc;

import java.awt.*;
import java.util.Random;

public class HelperFunctions {

  private static Random random = new Random();

  synchronized public static void setSeed(long seed){
    random.setSeed(seed);
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
  public static final String safeCharacters = safeAlphabets + safeNumbers + specialCharacters;

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

  synchronized public static int randomNumber(int min, int max) {
    return random.nextInt((max - min) + 1) + min;
  }

  synchronized public static int randomNumber(int bound) {
   return random.nextInt(bound);
  }
  
}
