package lc

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import scala.io.Source.fromFile
import lc.database.Statements
import lc.core.{Captcha, CaptchaProviders}
import lc.server.Server
import lc.background.BackgroundTask


object LCFramework{
  def main(args: scala.Array[String]) {
  	val captcha = new Captcha()
    val server = new Server(8888, captcha)
    val backgroudTask = new BackgroundTask(captcha, 10)
    backgroudTask.beginThread(2)
    server.start()
  }
}

object MakeSamples {
  def main(args: scala.Array[String]) {
    val samples = CaptchaProviders.generateChallengeSamples()
    samples.foreach {case (key, sample) =>
      val extensionMap = Map("image/png" -> "png", "image/gif" -> "gif")
      println(key + ": " + sample)

      val outStream = new java.io.FileOutputStream("samples/"+key+"."+extensionMap(sample.contentType))
      outStream.write(sample.content)
      outStream.close
    }
  }
}
