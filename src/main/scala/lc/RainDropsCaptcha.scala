package lc

import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.awt.Font
import java.awt.font.TextAttribute
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

class Drop {
  var x = 0
  var y = 0
  var yOffset = 0
  var color = 0
  var colorChange = 10
  def mkColor = {
    new Color(color, color,  200)
  }
}

class RainDropsCP extends ChallengeProvider {
  private val alphabet = "abcdefghijklmnopqrstuvwxyz"
  private val n = 6
  private val bgColor = new Color(245, 245, 245)
  private val textColor = new Color(248, 248, 248)
  private val textHighlightColor = new Color(208, 208, 255)

  def getId = "FilterChallenge"

  private def extendDrops(drops: Array[Drop], steps: Int, xOffset: Int) = {
    drops.map(d => {
      val nd = new Drop()
      nd.x + xOffset*steps
      nd.y + d.yOffset*steps
      nd
    })
  }

  def returnChallenge(): Challenge = {
    val r = new scala.util.Random
    val secret = Stream.continually(r.nextInt(alphabet.size)).map(alphabet).take(n).mkString
    // val secret = "mmmmmm"
    val width = 450
    val height = 100
    val imgType = BufferedImage.TYPE_INT_RGB
    val xOffset = 1+r.nextInt(2)
    val xBias = (height / 10) - 2
    val dropsOrig = Array.fill[Drop](600)( new Drop())
    for (d <- dropsOrig) {
      d.x = r.nextInt(width) - (xBias/2)*xOffset
      d.yOffset = 6+r.nextInt(6)
      d.y = r.nextInt(height)
      d.color = r.nextInt(240)
      if (d.color > 128) {
        d.colorChange *= -1
      }
    }
    val drops = dropsOrig ++ extendDrops(dropsOrig, 2, xOffset) ++ extendDrops(dropsOrig, 4, xOffset)

    val baseFont = new Font(Font.MONOSPACED, Font.BOLD, 80)
    val attributes = new java.util.HashMap[TextAttribute, Object]()
    attributes.put(TextAttribute.TRACKING, Double.box(0.2))
    attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD)
    val spacedFont = baseFont.deriveFont(attributes)

    val baos = new ByteArrayOutputStream();
    val ios = new MemoryCacheImageOutputStream(baos);
    val writer = new GifSequenceWriter(ios, imgType, 60, true);
    for(i <- 0 until 30){
      val yOffset = 5+r.nextInt(5)
      val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      val g = canvas.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      // clear the canvas
      g.setColor(bgColor)
      g.fillRect(0, 0, canvas.getWidth, canvas.getHeight)

      // paint the rain
      for (d <- drops) {
        g.setColor(d.mkColor)
        g.drawLine(d.x, d.y, d.x+xOffset, d.y+d.yOffset)
        d.x += xOffset
        d.y += d.yOffset
        d.color += d.colorChange
        if (d.x > width+xOffset || d.y > height+d.yOffset) {
          val ySteps = 1 + height / d.yOffset
          d.x -= xOffset*ySteps
          d.y -= yOffset*ySteps

        }
        if (d.color > 200 || d.color < 21) {
          d.colorChange *= -1
        }
      }

      // center the text
      g.setFont(spacedFont)
      val textWidth = g.getFontMetrics().charsWidth(secret.toCharArray, 0, secret.toCharArray.length)
      val textX = (width - textWidth)/2

      // paint the top outline
      g.setColor(textHighlightColor)
      g.drawString(secret, textX, 69)
      // paint the text
      g.setColor(textColor)
      g.drawString(secret, textX, 70)

      g.dispose()
      writer.writeToSequence(canvas)
    }
    writer.close
    ios.close

    // ImageIO.write(canvas,"png",baos);
    new Challenge(baos.toByteArray, "image/png", secret)
  }
  def checkAnswer(secret: String, answer: String): Boolean = {
    secret == answer
  }
}
