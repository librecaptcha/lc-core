import com.sksamuel.scrimage._
import java.io._

class CaptchaLibrary {
  var tokenAnswer = scala.collection.mutable.Map[String, String]()
  def init = {}
  def shutdown = {}
  def getChallenge(): Challenge = {
    //choose a captcha provider randomly
    val blurCaptcha = new BlurCaptcha
    val (challenge, answer) = blurCaptcha.getChallenge()
    tokenAnswer += challenge.token->answer
    challenge
  }
  def checkAnswer(token: String, input: String): Boolean = {
    if (tokenAnswer(token) == input) {
      true
    }
    else {
      false
    }
  }
}

trait CaptchaProvider {
  def getChallenge(): (Challenge, String)
}

class Challenge(val token: String, val image: Image)

class Answer(val token: String, val input: String)

class BlurCaptcha extends CaptchaProvider {
  def getChallenge(): (Challenge, String) = {
    val inFileName = "image2.png"
    var image = Image.fromStream(new FileInputStream(inFileName))
    image = image.filter(com.sksamuel.scrimage.filter.BlurFilter)
    image.output(new File("blur.png"))

    val r = scala.util.Random
    val token = r.nextInt(1000).toString
    val challenge = new Challenge(token, image)
    val answer = "about"
    (challenge, answer)
  }
}
