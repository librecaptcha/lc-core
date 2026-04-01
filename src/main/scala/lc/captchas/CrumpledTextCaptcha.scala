package lc.captchas

import com.sksamuel.scrimage._
import com.sksamuel.scrimage.filter._
import java.awt.image.BufferedImage
import java.awt.Font
import java.awt.Color
import java.awt.Graphics2D
import java.awt.BasicStroke
import java.awt.RenderingHints
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import java.util.{List => JavaList, Map => JavaMap}
import java.io.ByteArrayOutputStream
import lc.misc.PngImageWriter
import lc.misc.HelperFunctions

class CrumpledTextCaptcha extends ChallengeProvider {
  def getId = "CrumpledTextCaptcha"

  def configure(config: String): Unit = {
    // TODO: add custom config
  }

  def supportedParameters(): JavaMap[String, JavaList[String]] = {
    JavaMap.of(
      "supportedLevels",
      JavaList.of("easy", "medium", "hard"),
      "supportedMedia",
      JavaList.of("image/png"),
      "supportedInputType",
      JavaList.of("text")
    )
  }

  def returnChallenge(level: String, size: String): Challenge = {
    val r = new scala.util.Random
    val n = level match {
      case "easy" => 5
      case "medium" => 6
      case "hard" => 7
      case _ => 6
    }
    val characters = if (level == "hard") HelperFunctions.safeCharacters else HelperFunctions.safeAlphaNum
    val secret = LazyList.continually(r.nextInt(characters.size)).map(characters).take(n).mkString
    val size2D = HelperFunctions.parseSize2D(size)
    val width = size2D(0)
    val height = size2D(1)
    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    HelperFunctions.setRenderingHints(g)

    val fontHeight = (height * 0.5).toInt
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, width, height)

    // Draw background "noise" or "crumbs"
    for (_ <- 0 until 100) {
      g.setColor(new Color(200 + r.nextInt(55), 200 + r.nextInt(55), 200 + r.nextInt(55)))
      g.fillRect(r.nextInt(width), r.nextInt(height), 2, 2)
    }

    g.setColor(Color.BLACK)
    val font = new Font("Serif", Font.BOLD, fontHeight)
    g.setFont(font)

    val stringWidth = g.getFontMetrics().stringWidth(secret)
    val scaleX = if (stringWidth > width * 0.8) (width * 0.8) / (stringWidth.toDouble) else 1d
    val xOffset = ((width - (stringWidth * scaleX)) / 2).toInt
    val yOffset = ((height + fontHeight) / 2).toInt

    val oldTransform = g.getTransform()
    g.translate(xOffset, yOffset)
    g.scale(scaleX, 1d)

    // Draw characters with slight random rotation/offset
    var currentX = 0
    for (char <- secret) {
        val charTransform = g.getTransform()
        g.translate(currentX, r.nextInt(10) - 5)
        g.rotate((r.nextDouble() - 0.5) * 0.3)
        g.drawString(char.toString, 0, 0)
        currentX += g.getFontMetrics().charWidth(char)
        g.setTransform(charTransform)
    }

    g.setTransform(oldTransform)

    // Add random creases
    for (_ <- 0 until 20) {
      val x1 = r.nextInt(width)
      val y1 = r.nextInt(height)
      val x2 = r.nextInt(width)
      val y2 = r.nextInt(height)

      // Light crease
      g.setColor(new Color(180, 180, 180, 180))
      g.setStroke(new BasicStroke(r.nextFloat() * 2.0f))
      g.drawLine(x1, y1, x2, y2)

      // Darker edge of the crease
      g.setColor(new Color(100, 100, 100, 100))
      g.setStroke(new BasicStroke(0.5f))
      g.drawLine(x1 + 1, y1 + 1, x2 + 1, y2 + 1)
    }

    g.dispose()

    val image = ImmutableImage.fromAwt(canvas)

    val ripple = new RippleFilter(com.sksamuel.scrimage.filter.RippleType.Noise, 8.toFloat, 8.toFloat, 0.01f, 0.01f)
    val blur = new GaussianBlurFilter(1)

    val filteredImage = image.filter(ripple, blur)

    val img = filteredImage.awt()
    val baos = new ByteArrayOutputStream()
    PngImageWriter.write(baos, img);
    new Challenge(baos.toByteArray, "image/png", secret)
  }

  def checkAnswer(secret: String, answer: String): Boolean = {
    secret.toLowerCase == answer.toLowerCase
  }
}
