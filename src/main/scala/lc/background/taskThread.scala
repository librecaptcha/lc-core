package lc.background

import lc.database.Statements
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import lc.core.{CaptchaManager, Config}
import lc.core.{Parameters, Size}
import lc.misc.HelperFunctions

class BackgroundTask(config: Config, captchaManager: CaptchaManager) {

  private val task = new Runnable {
    def run(): Unit = {
      try {
        val mapIdGCPstmt = Statements.tlStmts.get.mapIdGCPstmt
        mapIdGCPstmt.setInt(1, config.captchaExpiryTimeLimit)
        mapIdGCPstmt.executeUpdate()

        val challengeGCPstmt = Statements.tlStmts.get.challengeGCPstmt
        challengeGCPstmt.executeUpdate()

        val allCombinations = allParameterCombinations()
        val requiredCountPerCombination = Math.max(1, (config.bufferCount * 1.01) / allCombinations.size).toInt

        for (param <- allCombinations) {
          if (!shutdownInProgress) {
            val countExisting = captchaManager.getCount(param).getOrElse(0)
            val countRequired = requiredCountPerCombination - countExisting
            if (countRequired > 0) {
              val countCreate = Math.min(1.0 + requiredCountPerCombination / 10.0, countRequired).toInt
              println(s"Creating $countCreate of $countRequired captchas for $param")

              for (i <- 0 until countCreate) {
                if (!shutdownInProgress) {
                  captchaManager.generateChallenge(param)
                }
              }
            }
          }
        }
      } catch { case exception: Exception => println(exception) }
    }
  }

  private def allParameterCombinations(): List[Parameters] = {
    (config.captchaConfig).flatMap { captcha =>
      (captcha.allowedLevels).flatMap { level =>
        (captcha.allowedMedia).flatMap { media =>
          (captcha.allowedInputType).flatMap { inputType =>
            (captcha.allowedSizes).map {size =>
              Parameters(level, media, inputType, size)
            }
          }
        }
      }
    }
  }

  private def getRandomParam(): Parameters = {
    val captcha = pickRandom(config.captchaConfig)
    val level = pickRandom(captcha.allowedLevels)
    val media = pickRandom(captcha.allowedMedia)
    val inputType = pickRandom(captcha.allowedInputType)
    val size = pickRandom(captcha.allowedSizes)

    Parameters(level, media, inputType, size)
  }

  private def pickRandom[T](list: List[T]): T = {
    list(HelperFunctions.randomNumber(list.size))
  }

  private val ex = new ScheduledThreadPoolExecutor(1)

  def beginThread(delay: Int): Unit = {
    ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

  @volatile var shutdownInProgress = false

  def shutdown(): Unit = {
    println("  Shutting down background task...")
    shutdownInProgress = true
    ex.shutdown()
    println("    Finished Shutting background task")
    println("  Shutting down DB...")
    Statements.tlStmts.get.shutdown.execute()
    println("    Finished shutting down db")
  }

}
