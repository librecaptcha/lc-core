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
        val requiredCountPerCombination = Math.max(1, (config.throttle * 1.01) / allCombinations.size).toInt

        for (param <- allCombinations) {
          val countExisting = captchaManager.getCount(param).getOrElse(0)
          val countRequired = requiredCountPerCombination - countExisting
          if (countRequired > 0) {
            val countCreate = Math.min(1.0 + requiredCountPerCombination/10.0, countRequired).toInt
            println(s"Creating $countCreate of $countRequired captchas for $param")

            for (i <- 0 until countCreate) {
              captchaManager.generateChallenge(param)
            }
          }
        }
      } catch { case exception: Exception => println(exception) }
    }
  }

  private def allParameterCombinations(): List[Parameters] = {
    (config.captchaConfig).flatMap {captcha =>
      (captcha.allowedLevels).flatMap {level =>
        (captcha.allowedMedia).flatMap {media =>
          (captcha.allowedInputType).map {inputType =>
            Parameters(level, media, inputType, Some(Size(0, 0)))
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

    Parameters(level, media, inputType, Some(Size(0, 0)))
  }

  private def pickRandom[T](list: List[T]): T = {
    list(HelperFunctions.randomNumber(list.size))
  }

  def beginThread(delay: Int): Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

}
