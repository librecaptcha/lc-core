package lc.captchas

import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.awt.Font
import java.awt.font.TextAttribute
import java.awt.Color
import java.io.ByteArrayOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import lc.misc.GifSequenceWriter
import java.util.{List => JavaList, Map => JavaMap}
import lc.misc.HelperFunctions

class Drop {
  var x = 0
  var y = 0
  var yOffset = 0
  var color = 0
  var colorChange = 10
  def mkColor: Color = {
    new Color(color, color, math.min(200, color + 100))
  }
}

class RainDropsCP extends ChallengeProvider {
  private val alphabet = "abcdefghijklmnopqrstuvwxyz"
  private val n = 6
  private val bgColor = new Color(200, 200, 200)
  private val textColor = new Color(208, 208, 218)
  private val textHighlightColor = new Color(100, 100, 125)

  def getId = "FilterChallenge"

  def configure(config: String): Unit = {
    // TODO: add custom config
  }

  def supportedParameters(): JavaMap[String, JavaList[String]] = {
    JavaMap.of(
      "supportedLevels",
      JavaList.of("medium", "easy"),
      "supportedMedia",
      JavaList.of("image/gif"),
      "supportedInputType",
      JavaList.of("text")
    )
  }

  private def extendDrops(drops: Array[Drop], steps: Int, xOffset: Int) = {
    drops.map(d => {
      val nd = new Drop()
      nd.x + xOffset * steps
      nd.y + d.yOffset * steps
      nd
    })
  }

  def returnChallenge(level: String, size: String): Challenge = {
    val r = new scala.util.Random
    val secret = LazyList.continually(r.nextInt(alphabet.size)).map(alphabet).take(n).mkString
    val size2D = HelperFunctions.parseSize2D(size)
    val width = size2D(0)
    val height = size2D(1)
    val imgType = BufferedImage.TYPE_INT_RGB
    val xOffset = 2 + r.nextInt(3)
    val xBias = (height / 10) - 2
    val dropsOrig = Array.fill[Drop](2000)(new Drop())
    for (d <- dropsOrig) {
      d.x = r.nextInt(width) - (xBias / 2) * xOffset
      d.yOffset = 6 + r.nextInt(6)
      d.y = r.nextInt(height)
      d.color = r.nextInt(240)
      if (d.color > 128) {
        d.colorChange *= -1
      }
    }
    val drops = dropsOrig ++ extendDrops(dropsOrig, 1, xOffset) ++ extendDrops(dropsOrig, 2, xOffset) ++ extendDrops(
      dropsOrig,
      3,
      xOffset
    )

    val fontHeight = (height * 0.5f).toInt
    val baseFont = new Font(Font.MONOSPACED, Font.BOLD, fontHeight)
    val attributes = new java.util.HashMap[TextAttribute, Object]()
    attributes.put(TextAttribute.TRACKING, Double.box(0.2))
    attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD)
    val spacedFont = baseFont.deriveFont(attributes)

    val baos = new ByteArrayOutputStream();
    val ios = new MemoryCacheImageOutputStream(baos);
    val writer = new GifSequenceWriter(ios, imgType, 60, true);
    for (_ <- 0 until 60) {
      // val yOffset = 5+r.nextInt(5)
      val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      val g = canvas.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      // clear the canvas
      g.setColor(bgColor)
      g.fillRect(0, 0, canvas.getWidth, canvas.getHeight)

      // paint the rain
      for (d <- drops) {
        g.setColor(d.mkColor)
        g.drawLine(d.x, d.y, d.x + xOffset, d.y + d.yOffset)
        d.x += xOffset / 2
        d.y += d.yOffset / 2
        d.color += d.colorChange
        if (d.x > width || d.y > height) {
          val ySteps = (height / d.yOffset) + 1
          d.x -= xOffset * ySteps
          d.y -= d.yOffset * ySteps

        }
        if (d.color > 200 || d.color < 21) {
          d.colorChange *= -1
        }
      }

      g.setFont(spacedFont)
      val textWidth = g.getFontMetrics().stringWidth(secret)
      val scaleX = if (textWidth > width) width / textWidth.toDouble else 1.0d
      g.scale(scaleX, 1)

      // center the text
      val textX = if (textWidth > width) 0 else ((width - textWidth) / 2)

      // this will be overlapped by the following text to show the top outline because of the offset
      val yOffset = (fontHeight*0.01).ceil.toInt
      g.setColor(textHighlightColor)
      g.drawString(secret, textX, (fontHeight*1.1).toInt - yOffset)

      // paint the text
      g.setColor(textColor)
      g.drawString(secret, textX, (fontHeight*1.1).toInt)

      g.dispose()
      writer.writeToSequence(canvas)
    }
    writer.close
    ios.close

    // ImageIO.write(canvas,"png",baos);
    new Challenge(baos.toByteArray, "image/gif", secret)
  }
  def checkAnswer(secret: String, answer: String): Boolean = {
    secret == answer
  }
}
