package lc.database

import java.sql.{Connection, DriverManager, Statement}

class DBConn() {
  val con: Connection =
    DriverManager.getConnection("jdbc:h2:./data/H2/captcha3;MAX_COMPACT_TIME=8000;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1", "sa", "")

  def getStatement(): Statement = {
    con.createStatement()
  }

  def closeConnection(): Unit = {
    con.close()
  }
}
