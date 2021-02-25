package lc

import lc.core.{Captcha, CaptchaProviders}
import lc.server.Server
import lc.background.BackgroundTask

object LCFramework {
  def main(args: scala.Array[String]): Unit = {
    val captcha = new Captcha()
    val server = new Server(8888, captcha)
    val backgroudTask = new BackgroundTask(captcha, 10)
    backgroudTask.beginThread(2)
    server.start()
  }
}

object MakeSamples {
  def main(args: scala.Array[String]): Unit = {
    val samples = CaptchaProviders.generateChallengeSamples()
    samples.foreach {
      case (key, sample) =>
        val extensionMap = Map("image/png" -> "png", "image/gif" -> "gif")
        println(key + ": " + sample)

        val outStream = new java.io.FileOutputStream("samples/" + key + "." + extensionMap(sample.contentType))
        outStream.write(sample.content)
        outStream.close
    }
  }
}
