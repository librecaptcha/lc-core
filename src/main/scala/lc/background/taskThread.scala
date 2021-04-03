package lc.background

import lc.database.Statements
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import lc.core.Captcha
import lc.core.{Parameters, Size}

class BackgroundTask(throttle: Int, timeLimit: Int) {

  val captcha = new Captcha()

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
          captcha.generateChallenge(Parameters("medium", "image/png", "text", Option(Size(0, 0))))
          throttleIn -= 1
        }
      } catch { case exception: Exception => println(exception.getStackTrace) }
    }
  }

  def beginThread(delay: Int): Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

}
