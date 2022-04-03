package lc.captchas

import com.sksamuel.scrimage._
import com.sksamuel.scrimage.filter._
import java.awt.image.BufferedImage
import java.awt.Font
import java.awt.Color
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import java.util.{List => JavaList, Map => JavaMap}
import java.io.ByteArrayOutputStream
import lc.misc.PngImageWriter
import lc.misc.HelperFunctions

class FilterChallenge extends ChallengeProvider {
  def getId = "FilterChallenge"

  def configure(config: String): Unit = {
    // TODO: add custom config
  }

  def supportedParameters(): JavaMap[String, JavaList[String]] = {
    JavaMap.of(
      "supportedLevels",
      JavaList.of("medium", "hard"),
      "supportedMedia",
      JavaList.of("image/png"),
      "supportedInputType",
      JavaList.of("text")
    )
  }

  private val filterTypes = List(new FilterType1, new FilterType2)

  def returnChallenge(level: String, size: String): Challenge = {
    val mediumLevel = level == "medium"
    val r = new scala.util.Random
    val characters = if (mediumLevel) HelperFunctions.safeAlphaNum else HelperFunctions.safeCharacters
    val n = if (mediumLevel) 5 else 7
    val secret = LazyList.continually(r.nextInt(characters.size)).map(characters).take(n).mkString
    val size2D = HelperFunctions.parseSize2D(size)
    val width = size2D(0)
    val height = size2D(1)
    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    val fontHeight = (height*0.6).toInt
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, canvas.getWidth, canvas.getHeight)
    g.setColor(Color.BLACK)
    val font = new Font("Serif", Font.PLAIN, fontHeight)
    g.setFont(font)
    val stringWidth = g.getFontMetrics().stringWidth(secret)
    val scaleX = if (stringWidth > width) width/(stringWidth.toDouble) else 1d
    val margin = if (stringWidth > width) 0 else (width - stringWidth)
    val xOffset = (margin*r.nextDouble).toInt
    g.scale(scaleX, 1d)
    g.drawString(secret, xOffset, fontHeight)
    g.dispose()
    var image = ImmutableImage.fromAwt(canvas)
    val s = r.nextInt(2)
    image = filterTypes(s).applyFilter(image, !mediumLevel)
    val img = image.awt()
    val baos = new ByteArrayOutputStream()
    PngImageWriter.write(baos, img);
    new Challenge(baos.toByteArray, "image/png", secret)
  }
  def checkAnswer(secret: String, answer: String): Boolean = {
    secret == answer
  }
}

trait FilterType {
  def applyFilter(image: ImmutableImage, hardLevel: Boolean): ImmutableImage
}

class FilterType1 extends FilterType {
  override def applyFilter(image: ImmutableImage, hardLevel: Boolean): ImmutableImage = {
    val radius = if (hardLevel) 3 else 2
    val blur = new GaussianBlurFilter(radius)
    val smear = new SmearFilter(com.sksamuel.scrimage.filter.SmearType.Circles, 10, 10, 10, 0, 1)
    val diffuse = new DiffuseFilter(radius.toFloat)
    blur.apply(image)
    diffuse.apply(image)
    smear.apply(image)
    image
  }
}

class FilterType2 extends FilterType {
  override def applyFilter(image: ImmutableImage, hardLevel: Boolean): ImmutableImage = {
    val radius = if (hardLevel) 2f else 1f
    val smear = new SmearFilter(com.sksamuel.scrimage.filter.SmearType.Circles, 10, 10, 10, 0, 1)
    val diffuse = new DiffuseFilter(radius)
    val ripple = new RippleFilter(com.sksamuel.scrimage.filter.RippleType.Noise, 1, 1, 0.005.toFloat, 0.005.toFloat)
    diffuse.apply(image)
    ripple.apply(image)
    smear.apply(image)
    image
  }
}
