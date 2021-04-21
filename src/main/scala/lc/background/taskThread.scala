package lc.background

import lc.database.Statements
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import lc.core.{Captcha, Config}
import lc.core.{Parameters, Size}

class BackgroundTask(throttle: Int, timeLimit: Int) {

  private val task = new Runnable {
    def run(): Unit = {
      try {
        val mapIdGCPstmt = Statements.tlStmts.get.mapIdGCPstmt
        mapIdGCPstmt.setInt(1, timeLimit)
        mapIdGCPstmt.executeUpdate()

        val challengeGCPstmt = Statements.tlStmts.get.challengeGCPstmt
        challengeGCPstmt.executeUpdate()

        val imageNum = Statements.tlStmts.get.getCountChallengeTable.executeQuery()
        var throttleIn = (throttle * 1.1).toInt
        if (imageNum.next())
          throttleIn = (throttleIn - imageNum.getInt("total"))
        while (0 < throttleIn) {
          Captcha.generateChallenge(getRandomParam())
          throttleIn -= 1
        }
      } catch { case exception: Exception => println(exception) }
    }
  }

  private def getRandomParam(): Parameters = {
    val captcha = pickRandom(Config.captchaConfig)
    val level = pickRandom(captcha.allowedLevels)
    val media = pickRandom(captcha.allowedMedia)
    val inputType = pickRandom(captcha.allowedInputType)

    Parameters(level, media, inputType, Some(Size(0, 0)))
  }

  private def pickRandom[T](list: List[T]): T = {
    list(Config.getNextRandomInt(list.size))
  }

  def beginThread(delay: Int): Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

}
