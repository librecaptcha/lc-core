import com.sksamuel.scrimage._
import java.io._

class CaptchaLibrary {
  val captchas = List(new BlurCaptcha, new LabelCaptcha)
  var tokenCaptcha = scala.collection.mutable.Map[String, CaptchaProvider]()
  def init = {}
  def shutdown = {}
  def getChallenge(): Challenge = {
    //choose a captcha provider randomly
    val r = scala.util.Random.nextInt(2)
    val captchaInstance = captchas(r)
    val challenge = captchaInstance.getChallenge()
    tokenCaptcha += challenge.token -> captchaInstance
    challenge
  }
  def checkAnswer(token: String, input: String): Boolean = {
    val result = tokenCaptcha(token).checkAnswer(token, input)
    result
  }
}

trait CaptchaProvider {
  def getChallenge(): (Challenge)
  def checkAnswer(token: String, input: String): Boolean
}

class Challenge(val token: String, val image: Image)

class Answer(val token: String, val input: String)

object LibreCaptcha {
  def main(args: Array[String]) {
    val captcha = new CaptchaLibrary
    var a = 0
    for (a <- 1 to 3)
    {
      val challenge = captcha.getChallenge()
      println(s"Token: ${challenge.token}")
      challenge.image.output(new File("Captcha.png"))
      println("Enter your answer: ")
      val input = scala.io.StdIn.readLine()
      val result = captcha.checkAnswer(challenge.token, input)
      println(s"Result: $result")
    }
  }
}
