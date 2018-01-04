import com.sksamuel.scrimage._
import com.sksamuel.scrimage.filter.GaussianBlurFilter
import java.io._

class BlurCaptcha extends CaptchaProvider {
  val tokenAnswer = scala.collection.mutable.Map[String, String]()
  val imageFiles = new File("known").listFiles.toList
  def getChallenge(): (Challenge) = {
    val r = scala.util.Random.nextInt(imageFiles.length)
    val chosenImage = imageFiles(r)
    var image = Image.fromStream(new FileInputStream(chosenImage))
    val blur = new GaussianBlurFilter(5)
    blur.apply(image)
    val s = scala.util.Random
    val token = s.nextInt(1000).toString
    val challenge = new Challenge(token, image)
    val answer = "about"
    tokenAnswer += token -> answer
    challenge
  }
  def checkAnswer(token: String, input: String): Boolean = {
    if(tokenAnswer(token)==input)
    {
      true
    }
    else
    {
      false
    }
  }
}
