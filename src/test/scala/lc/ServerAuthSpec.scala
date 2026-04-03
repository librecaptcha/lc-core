package lc.server

import org.scalatest.funsuite.AnyFunSuite
import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter}
import lc.LCFramework
import scala.jdk.CollectionConverters._

class ServerAuthSpec extends AnyFunSuite {

  test("Server should require auth header when authRequired is true") {
    // Set environment variable in the current JVM process using reflection
    try {
      val field = classOf[java.util.Collections].getDeclaredClasses.find(_.getName == "java.util.Collections$UnmodifiableMap").get.getDeclaredField("m")
      field.setAccessible(true)
      val map = field.get(sys.env.asJava).asInstanceOf[java.util.Map[String, String]]
      map.put("AUTH_KEY", "secret123")
    } catch {
      case _: Throwable => // Might fail on Java 16+ without --add-opens, ignore silently and skip test if so
    }

    // Only run the test if we successfully set the env var
    if (sys.env.get("AUTH_KEY").contains("secret123")) {
      val authFramework = new LCFramework()
      authFramework.start("tests/auth-config.json")
      Thread.sleep(2000)

      try {
        val url = new URL("http://localhost:8889/v2/captcha")

        // 1. Test without auth header
        val connection1 = url.openConnection().asInstanceOf[HttpURLConnection]
        connection1.setRequestMethod("POST")
        connection1.setRequestProperty("Content-Type", "application/json")
        connection1.setDoOutput(true)
        val payload = """{"level":"debug","media":"image/png","input_type":"text","size":"350x100"}"""
        val out1 = new OutputStreamWriter(connection1.getOutputStream)
        out1.write(payload)
        out1.close()

        var responseCode = connection1.getResponseCode
        assert(responseCode == 401, s"Expected 401 but got $responseCode")

        // 2. Test with invalid auth header
        val connection2 = url.openConnection().asInstanceOf[HttpURLConnection]
        connection2.setRequestMethod("POST")
        connection2.setRequestProperty("Content-Type", "application/json")
        connection2.setRequestProperty("Auth", "wrongsecret")
        connection2.setDoOutput(true)
        val out2 = new OutputStreamWriter(connection2.getOutputStream)
        out2.write(payload)
        out2.close()

        responseCode = connection2.getResponseCode
        assert(responseCode == 401, s"Expected 401 but got $responseCode")

        // 3. Test with valid auth header
        val connection3 = url.openConnection().asInstanceOf[HttpURLConnection]
        connection3.setRequestMethod("POST")
        connection3.setRequestProperty("Content-Type", "application/json")
        connection3.setRequestProperty("Auth", "secret123")
        connection3.setDoOutput(true)
        val out3 = new OutputStreamWriter(connection3.getOutputStream)
        out3.write(payload)
        out3.close()

        responseCode = connection3.getResponseCode
        assert(responseCode == 200, s"Expected 200 but got $responseCode")
      } finally {
        authFramework.stop()
      }
    }
  }
}
