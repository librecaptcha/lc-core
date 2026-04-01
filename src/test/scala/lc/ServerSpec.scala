package lc.server

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter}
import lc.LCFramework

class ServerSpec extends AnyFunSuite with BeforeAndAfterAll {

  val framework = new LCFramework()

  override def beforeAll(): Unit = {
    framework.start("tests/debug-config.json")
    // Give the server a moment to start and generate some captchas
    Thread.sleep(2000)
  }

  override def afterAll(): Unit = {
    framework.stop()
  }

  test("Server should respond with an id for a valid captcha request") {
    val url = new URL("http://localhost:8888/v2/captcha")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setDoOutput(true)

    val payload = """{"level":"debug","media":"image/png","input_type":"text","size":"350x100"}"""
    val out = new OutputStreamWriter(connection.getOutputStream)
    out.write(payload)
    out.close()

    val responseCode = connection.getResponseCode
    assert(responseCode == 200, s"Expected 200 but got $responseCode")

    val in = new BufferedReader(new InputStreamReader(connection.getInputStream))
    val response = new StringBuilder
    var line: String = in.readLine()
    while (line != null) {
      response.append(line)
      line = in.readLine()
    }
    in.close()

    val responseString = response.toString()
    assert(responseString.contains("id"), "Response did not contain an id")
  }
}
