package lc.captchas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import lc.captchas.interfaces.Challenge;
import lc.captchas.interfaces.ChallengeProvider;
import lc.misc.GifSequenceWriter;
import lc.misc.HelperFunctions;

public class PoppingCharactersCaptcha implements ChallengeProvider {

  private int[] computeOffsets(
      final Font font, final int width, final int height, final String text) {
    final var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final var graphics2D = img.createGraphics();
    final var frc = graphics2D.getFontRenderContext();
    final var advances = new int[text.length() + 1];
    final var spacing = font.getStringBounds(" ", frc).getWidth() / 3;
    var currX = 0;
    for (int i = 0; i < text.length(); i++) {
      final var c = text.charAt(i);
      advances[i] = currX;
      currX += font.getStringBounds(String.valueOf(c), frc).getWidth();
      currX += spacing;
    }
    ;
    advances[text.length()] = currX;
    graphics2D.dispose();
    return advances;
  }

  private BufferedImage makeImage(
      final Font font, final int width, final int height, final Consumer<Graphics2D> f) {
    final var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final var graphics2D = img.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics2D.setFont(font);
    f.accept(graphics2D);
    graphics2D.dispose();
    return img;
  }

  private int jitter() {
    return HelperFunctions.randomNumber(-2, +2);
  }

  private byte[] gifCaptcha(final int width, final int height, final String text) {
    try {
      final var fontHeight = (int) (height * 0.5);
      final Font font = new Font("Arial", Font.ROMAN_BASELINE, fontHeight);
      final var byteArrayOutputStream = new ByteArrayOutputStream();
      final var output = new MemoryCacheImageOutputStream(byteArrayOutputStream);
      final var writer = new GifSequenceWriter(output, 1, 900, true);
      final var advances = computeOffsets(font, width, height, text);
      final var expectedWidth = advances[advances.length - 1];
      final var scale = width / (float) expectedWidth;
      final var prevColor = Color.getHSBColor(0f, 0f, 0.1f);
      IntStream.range(0, text.length())
          .forEach(
              i -> {
                final var color =
                    Color.getHSBColor(HelperFunctions.randomNumber(0, 100) / 100.0f, 0.6f, 1.0f);
                final var nextImage =
                    makeImage(
                        font,
                        width,
                        height,
                        (g) -> {
                          g.scale(scale, 1);
                          if (i > 0) {
                            final var prevI = (i - 1) % text.length();
                            g.setColor(prevColor);
                            g.drawString(
                                String.valueOf(text.charAt(prevI)),
                                advances[prevI] + jitter(),
                                fontHeight * 1.1f + jitter());
                          }
                          g.setColor(color);
                          g.drawString(
                              String.valueOf(text.charAt(i)),
                              advances[i] + jitter(),
                              fontHeight * 1.1f + jitter());
                        });
                try {
                  writer.writeToSequence(nextImage);
                } catch (final IOException e) {
                  e.printStackTrace();
                }
              });
      writer.close();
      output.close();
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void configure(final String config) {
    // TODO: Add custom config
  }

  public Map<String, List<String>> supportedParameters() {
    return Map.of(
        "supportedLevels", List.of("hard"),
        "supportedMedia", List.of("image/gif"),
        "supportedInputType", List.of("text"));
  }

  public Challenge returnChallenge(String level, String size) {
    final var secret = HelperFunctions.randomString(6);
    final int[] size2D = HelperFunctions.parseSize2D(size);
    final int width = size2D[0];
    final int height = size2D[1];
    return new Challenge(gifCaptcha(width, height, secret), "image/gif", secret.toLowerCase());
  }

  public boolean checkAnswer(String secret, String answer) {
    return answer.toLowerCase().equals(secret);
  }

  public String getId() {
    return "PoppingCharactersCaptcha";
  }
}
