package lc.captchas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.List;

import lc.misc.HelperFunctions;
import lc.misc.PngImageWriter;
import lc.captchas.interfaces.Challenge;
import lc.captchas.interfaces.ChallengeProvider;

public class ShadowTextCaptcha implements ChallengeProvider {

  public String getId() {
    return "ShadowTextCaptcha";
  }

  public void configure(String config) {
    // TODO: Add custom config
  }

  public Map<String, List<String>> supportedParameters() {
    return Map.of(
        "supportedLevels", List.of("easy"),
        "supportedMedia", List.of("image/png"),
        "supportedInputType", List.of("text"));
  }

  public boolean checkAnswer(String secret, String answer) {
    return answer.toLowerCase().equals(secret);
  }

  private float[] makeKernel(int size) {
    final int N = size * size;
    final float weight = 1.0f / (N);
    final float[] kernel = new float[N];
    java.util.Arrays.fill(kernel, weight);
    return kernel;
  };

  private byte[] shadowText(final int width, final int height, String text) {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final int fontHeight = (int) (height * 0.5f);
    Font font = new Font("Arial", Font.PLAIN, fontHeight);
    Graphics2D graphics2D = img.createGraphics();
    HelperFunctions.setRenderingHints(graphics2D);
    graphics2D.setPaint(Color.WHITE);
    graphics2D.fillRect(0, 0, width, height);
    graphics2D.setPaint(Color.BLACK);
    graphics2D.setFont(font);
    final var stringWidth = graphics2D.getFontMetrics().stringWidth(text);
    final var scaleX = (stringWidth > width) ? width/((double) stringWidth) : 1d;
    graphics2D.scale(scaleX, 1d);
    graphics2D.drawString(text, 0, fontHeight*1.1f);
    graphics2D.dispose();
    final int kernelSize = (int) Math.ceil((Math.min(width, height) / 50.0));
    ConvolveOp op = new ConvolveOp(new Kernel(kernelSize, kernelSize, makeKernel(kernelSize)), ConvolveOp.EDGE_NO_OP, null);
    BufferedImage img2 = op.filter(img, null);
    Graphics2D g2d = img2.createGraphics();
    HelperFunctions.setRenderingHints(g2d);
    g2d.setPaint(Color.WHITE);
    g2d.scale(scaleX, 1d);
    g2d.setFont(font);
    g2d.drawString(text, -kernelSize, fontHeight*1.1f);
    g2d.dispose();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      PngImageWriter.write(baos, img2);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return baos.toByteArray();
  }

  public Challenge returnChallenge(String level, String size) {
    String secret = HelperFunctions.randomString(6);
    final int[] size2D = HelperFunctions.parseSize2D(size);
    final int width = size2D[0];
    final int height = size2D[1];
    return new Challenge(shadowText(width, height, secret), "image/png", secret.toLowerCase());
  }
}
