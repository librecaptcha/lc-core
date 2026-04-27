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
      case "easy"   => 5
      case "medium" => 6
      case "hard"   => 7
      case _        => 6
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
    g.drawString(secret, 0, 0)
    g.setTransform(oldTransform)
    g.dispose()

    var img = canvas
    val numFolds = level match {
      case "easy"   => 7
      case "medium" => 14
      case "hard"   => 19
      case _        => 14
    }
    for (_ <- 0 until numFolds) {
      img = applyFold(img, r)
    }

    val baos = new ByteArrayOutputStream()
    PngImageWriter.write(baos, img);
    new Challenge(baos.toByteArray, "image/png", secret)
  }

  private def applyFold(img: BufferedImage, r: scala.util.Random): BufferedImage = {
    val width = img.getWidth
    val height = img.getHeight
    val newImg = new BufferedImage(width, height, img.getType)

    val x1 = r.nextInt(width)
    val y1 = r.nextInt(height)
    val angle = r.nextDouble() * 2 * Math.PI
    val L = (r.nextDouble() * 0.5 + 0.3) * width
    val W = (r.nextDouble() * 0.3 + 0.1) * height

    val dx = Math.cos(angle)
    val dy = Math.sin(angle)
    val nx = -dy
    val ny = dx

    val maxShift = (r.nextDouble() - 0.5) * 20.0

    for (y <- 0 until height) {
      for (x <- 0 until width) {
        val px = x - x1
        val py = y - y1
        val projS = px * dx + py * dy
        val projN = px * nx + py * ny

        if (projS >= 0 && projS <= L && Math.abs(projN) <= W) {
          val shift = (projN / W) * maxShift
          val srcX = x - shift * dx
          val srcY = y - shift * dy

          if (srcX >= 0 && srcX < width - 1 && srcY >= 0 && srcY < height - 1) {
            val xf = Math.floor(srcX).toInt
            val yf = Math.floor(srcY).toInt
            val xt = srcX - xf
            val yt = srcY - yf

            val rgb00 = img.getRGB(xf, yf)
            val rgb10 = img.getRGB(xf + 1, yf)
            val rgb01 = img.getRGB(xf, yf + 1)
            val rgb11 = img.getRGB(xf + 1, yf + 1)

            val r00 = (rgb00 >> 16) & 0xff
            val g00 = (rgb00 >> 8) & 0xff
            val b00 = rgb00 & 0xff
            val r10 = (rgb10 >> 16) & 0xff
            val g10 = (rgb10 >> 8) & 0xff
            val b10 = rgb10 & 0xff
            val r01 = (rgb01 >> 16) & 0xff
            val g01 = (rgb01 >> 8) & 0xff
            val b01 = rgb01 & 0xff
            val r11 = (rgb11 >> 16) & 0xff
            val g11 = (rgb11 >> 8) & 0xff
            val b11 = rgb11 & 0xff

            val rInterp = (r00 * (1 - xt) * (1 - yt) + r10 * xt * (1 - yt) + r01 * (1 - xt) * yt + r11 * xt * yt).toInt
            val gInterp = (g00 * (1 - xt) * (1 - yt) + g10 * xt * (1 - yt) + g01 * (1 - xt) * yt + g11 * xt * yt).toInt
            val bInterp = (b00 * (1 - xt) * (1 - yt) + b10 * xt * (1 - yt) + b01 * (1 - xt) * yt + b11 * xt * yt).toInt

            val rgb = (0xff << 24) | (rInterp << 16) | (gInterp << 8) | bInterp
            newImg.setRGB(x, y, rgb)
          } else {
            newImg.setRGB(x, y, 0xffffffff)
          }
        } else {
          newImg.setRGB(x, y, img.getRGB(x, y))
        }
      }
    }
    newImg
  }

  def checkAnswer(secret: String, answer: String): Boolean = {
    secret.toLowerCase == answer.toLowerCase
  }
}
