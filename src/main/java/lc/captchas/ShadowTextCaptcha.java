package lc.captchas;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

import lc.misc.HelperFunctions;
import lc.captchas.interfaces.Challenge;
import lc.captchas.interfaces.ChallengeProvider;

public class ShadowTextCaptcha implements ChallengeProvider {

  public String getId() {
    return "ShadowTextCaptcha";
  }

  public void configure(String config) {
    // TODO: Add custom config
  }

  public HashMap<String, List<String>> supportedParameters() {
    HashMap<String, List<String>> supportedParams = new HashMap<String, List<String>>();
    supportedParams.put("supportedLevels", List.of("easy"));
    supportedParams.put("supportedMedia", List.of("image/png"));
    supportedParams.put("supportedInputType", List.of("text"));

    return supportedParams;
  }

  public boolean checkAnswer(String secret, String answer) {
    return answer.toLowerCase().equals(secret);
  }

  private byte[] shadowText(String text) {
    BufferedImage img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB);
    Font font = new Font("Arial", Font.ROMAN_BASELINE, 48);
    Graphics2D graphics2D = img.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    TextLayout textLayout = new TextLayout(text, font, graphics2D.getFontRenderContext());
    HelperFunctions.setRenderingHints(graphics2D);
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
    ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
    BufferedImage img2 = op.filter(img, null);
    Graphics2D g2d = img2.createGraphics();
    HelperFunctions.setRenderingHints(g2d);
    g2d.setPaint(Color.WHITE);
    textLayout.draw(g2d, 13, 50);
    g2d.dispose();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(img2, "png", baos);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return baos.toByteArray();
  }

  public Challenge returnChallenge() {
    String secret = HelperFunctions.randomString(6);
    return new Challenge(shadowText(secret), "image/png", secret.toLowerCase());
  }
}
