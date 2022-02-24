package lc.background

import lc.database.Statements
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import lc.core.{Captcha, Config}
import lc.core.{Parameters, Size}
import lc.misc.HelperFunctions

class BackgroundTask(config: Config, captcha: Captcha) {

  private val task = new Runnable {
    def run(): Unit = {
      try {
        val mapIdGCPstmt = Statements.tlStmts.get.mapIdGCPstmt
        mapIdGCPstmt.setInt(1, config.captchaExpiryTimeLimit)
        mapIdGCPstmt.executeUpdate()

        val challengeGCPstmt = Statements.tlStmts.get.challengeGCPstmt
        challengeGCPstmt.executeUpdate()

        val imageNumResult = Statements.tlStmts.get.getCountChallengeTable.executeQuery()
        val imageNum = if (imageNumResult.next()) {
          imageNumResult.getInt("total")
        } else {
          0
        }

        val throttle = (config.throttle * 1.1).toInt - imageNum

        for (i <- 0 until throttle) {
          captcha.generateChallenge(getRandomParam())
        }
      } catch { case exception: Exception => println(exception) }
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
