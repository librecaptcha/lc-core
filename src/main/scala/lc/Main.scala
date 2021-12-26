package lc

import lc.core.{CaptchaProviders, Captcha, Config}
import lc.server.Server
import lc.background.BackgroundTask
import lc.database.Statements

object LCFramework {
  def main(args: scala.Array[String]): Unit = {
    val configFilePath = if (args.length > 0) {
      args(0)
    } else {
      "data/config.json"
    }
    val config = new Config(configFilePath)
    Statements.maxAttempts = config.maxAttempts
    val captchaProviders = new CaptchaProviders(config = config)
    val captcha = new Captcha(config = config, captchaProviders = captchaProviders)
    val backgroundTask = new BackgroundTask(config = config, captcha = captcha)
    backgroundTask.beginThread(delay = config.threadDelay)
    val server = new Server(
      address = config.address,
      port = config.port,
      captcha = captcha,
      playgroundEnabled = config.playgroundEnabled,
      corsHeader = config.corsHeader
    )
    server.start()
  }
}

object MakeSamples {
  def main(args: scala.Array[String]): Unit = {
    val configFilePath = if (args.length > 0) {
      args(0)
    } else {
      "data/config.json"
    }
    val config = new Config(configFilePath)
    val captchaProviders = new CaptchaProviders(config = config)
    val samples = captchaProviders.generateChallengeSamples()
    samples.foreach { case (key, sample) =>
      val extensionMap = Map("image/png" -> "png", "image/gif" -> "gif")
      println(key + ": " + sample)

      val outStream = new java.io.FileOutputStream("samples/" + key + "." + extensionMap(sample.contentType))
      outStream.write(sample.content)
      outStream.close
    }
  }
}
