package lc.captchas

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Font
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Map
import java.util.List

import lc.misc.HelperFunctions
import lc.captchas.interfaces.Challenge
import lc.captchas.interfaces.ChallengeProvider

/** This captcha is only for debugging purposes. It creates very simple captchas that are deliberately easy to solve with OCR engines */
class DebugCaptcha extends ChallengeProvider {

  def getId(): String = {
    "DebugCaptcha"
  }

  def configure(config: String): Unit = {
    // TODO: Add custom config
  }

  def supportedParameters(): Map[String, List[String]] = {
    Map.of(
      "supportedLevels", List.of("debug"),
      "supportedMedia", List.of("image/png"),
      "supportedInputType", List.of("text")
    )
  }

  def checkAnswer(secret: String, answer: String): Boolean = {
    answer.toLowerCase().equals(secret)
  }

  private def simpleText(text: String): Array[Byte] = {
    val img = new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB)
    val font = new Font("Arial", Font.ROMAN_BASELINE, 56)
    val graphics2D = img.createGraphics()
    val textLayout = new TextLayout(text, font, graphics2D.getFontRenderContext())
    HelperFunctions.setRenderingHints(graphics2D)
    graphics2D.setPaint(Color.WHITE)
    graphics2D.fillRect(0, 0, 350, 100)
    graphics2D.setPaint(Color.BLACK)
    textLayout.draw(graphics2D, 15, 50)
    graphics2D.dispose()
    val baos = new ByteArrayOutputStream()
    try {
      ImageIO.write(img, "png", baos)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    baos.toByteArray()
  }

  def returnChallenge(): Challenge = {
    val secret = HelperFunctions.randomString(6, HelperFunctions.alphabets)
    new Challenge(simpleText(secret), "image/png", secret.toLowerCase())
  }
}
