package lc

import lc.core.{CaptchaProviders, CaptchaManager, Config}
import lc.server.Server
import lc.background.BackgroundTask
import lc.database.Statements

class LCFramework {
  private var backgroundTask: Option[BackgroundTask] = None
  private var server: Option[Server] = None

  def start(configFilePath: String = "data/config.json"): Unit = {
    val config = new Config(configFilePath)

    if (config.authRequired && sys.env.get("AUTH_KEY").isEmpty) {
      throw new Exception("AUTH_KEY environment variable is not specified, but authRequired is true.")
    }

    Statements.maxAttempts = config.maxAttempts
    val captchaProviders = new CaptchaProviders(config = config)
    val captchaManager = new CaptchaManager(config = config, captchaProviders = captchaProviders)
    val task = new BackgroundTask(config = config, captchaManager = captchaManager)
    task.beginThread(delay = config.threadDelay)
    backgroundTask = Some(task)

    val srv = new Server(
      address = config.address,
      port = config.port,
      captchaManager = captchaManager,
      playgroundEnabled = config.playgroundEnabled,
      corsHeader = config.corsHeader,
      authRequired = config.authRequired
    )
    srv.start()
    server = Some(srv)
  }

  def stop(): Unit = {
    println("Shutting down gracefully...")
    backgroundTask.foreach(_.shutdown())
    server.foreach(_.stop())
  }
}

object LCFramework {
  def main(args: scala.Array[String]): Unit = {
    val configFilePath = if (args.length > 0) {
      args(0)
    } else {
      "data/config.json"
    }

    val framework = new LCFramework()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = {
        framework.stop()
      }
    })

    framework.start(configFilePath)
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
