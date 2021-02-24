package lc.background

import lc.database.Statements
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import lc.core.Captcha
import lc.core.{Parameters, Size}


class BackgroundTask(captcha: Captcha, throttle: Int) {

    private val task = new Runnable {
        def run(): Unit = {
            try {

                val mapIdGCPstmt = Statements.tlStmts.get.mapIdGCPstmt
                mapIdGCPstmt.executeUpdate()

                val challengeGCPstmt = Statements.tlStmts.get.challengeGCPstmt
                challengeGCPstmt.executeUpdate()

                val imageNum = Statements.tlStmts.get.getCountChallengeTable.executeQuery()
                var throttleIn = (throttle*1.1).toInt
                if(imageNum.next())
                    throttleIn = (throttleIn-imageNum.getInt("total"))
                while(0 < throttleIn){
                    captcha.generateChallenge(Parameters("","","",Option(Size(0,0))))
                    throttleIn -= 1
                }
            } catch { case e: Exception => println(e) }
        }
    }

  def beginThread(delay: Int) : Unit = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val thread = ex.scheduleWithFixedDelay(task, 1, delay, TimeUnit.SECONDS)
  }

}