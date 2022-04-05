package lc

import lc.core.{CaptchaProviders, CaptchaManager, Config}
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
    val captchaManager = new CaptchaManager(config = config, captchaProviders = captchaProviders)
    val backgroundTask = new BackgroundTask(config = config, captchaManager = captchaManager)
    backgroundTask.beginThread(delay = config.threadDelay)
    val server = new Server(
      address = config.address,
      port = config.port,
      captchaManager = captchaManager,
      playgroundEnabled = config.playgroundEnabled,
      corsHeader = config.corsHeader
    )

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = {
        println("Shutting down gracefully...")
        backgroundTask.shutdown()
      }
    })

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
