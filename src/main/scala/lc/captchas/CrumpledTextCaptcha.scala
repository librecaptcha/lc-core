package lc.captchas

import com.sksamuel.scrimage._
import com.sksamuel.scrimage.filter._
import java.awt.image.BufferedImage
import java.awt.Font
import java.awt.Color
import java.awt.Graphics2D
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

    // Draw characters with random rotation and vertical offset to simulate being on different paper folds
    var currentX = 0
    for (char <- secret) {
        val charTransform = g.getTransform()
        g.translate(currentX, r.nextInt(height / 4) - height / 8)
        g.rotate((r.nextDouble() - 0.5) * 1.0)
        g.drawString(char.toString, 0, 0)
        currentX += g.getFontMetrics().charWidth(char)
        g.setTransform(charTransform)
    }

    g.setTransform(oldTransform)
    g.dispose()

    val image = ImmutableImage.fromAwt(canvas)

    // Use a Triangle ripple to create sharp "folds" in the image
    val ripple = new RippleFilter(com.sksamuel.scrimage.filter.RippleType.Triangle, 10.toFloat, 10.toFloat, 0.05f, 0.05f)

    val filteredImage = image.filter(ripple)

    val img = filteredImage.awt()
    val baos = new ByteArrayOutputStream()
    PngImageWriter.write(baos, img);
    new Challenge(baos.toByteArray, "image/png", secret)
  }

  def checkAnswer(secret: String, answer: String): Boolean = {
    secret.toLowerCase == answer.toLowerCase
  }
}
