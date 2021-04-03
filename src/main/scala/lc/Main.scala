package lc

import lc.core.CaptchaProviders
import lc.server.Server
import lc.background.BackgroundTask
import lc.core.Config

object LCFramework {
  def main(args: scala.Array[String]): Unit = {
    val backgroundTask = new BackgroundTask(
      throttle = Config.throttle,
      timeLimit = Config.captchaExpiryTimeLimit
    )
    backgroundTask.beginThread(delay = Config.threadDelay)
    new Server(port = Config.port)
    println("Starting server on port:" + Config.port)
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
