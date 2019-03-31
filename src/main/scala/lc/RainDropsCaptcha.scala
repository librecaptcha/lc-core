package lc

import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.awt.Font
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class RainDropsCP extends ChallengeProvider {
  private val alphabet = "abcdefghijklmnopqrstuvwxyz"
  private val n = 8

  def getId = "FilterChallenge"

  def returnChallenge(): Challenge = {
    val r = new scala.util.Random
    val secret = Stream.continually(r.nextInt(alphabet.size)).map(alphabet).take(n).mkString
    val width = 225
    val height = 100

    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    // clear the canvas
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, canvas.getWidth, canvas.getHeight)

    // paint the rain
    g.setColor(Color.BLACK)
    val xOffset = 1+r.nextInt(10)
    val yOffset = 1+r.nextInt(10)
    for (i <- 0 until 1000) {
      val x = r.nextInt(width)
      val y = r.nextInt(height)
      g.drawLine(x, y, x+xOffset, y+yOffset)
    }
    
    // paint the text
    g.setColor(Color.WHITE)
    g.setFont(new Font("Sans", Font.BOLD, 42))
    g.drawString(secret, 5, 50)

    g.dispose()
    val baos = new ByteArrayOutputStream();
    ImageIO.write(canvas,"png",baos);
    new Challenge(baos.toByteArray, "image/png", secret)
  }
  def checkAnswer(secret: String, answer: String): Boolean = {
    secret == answer
  }
}
