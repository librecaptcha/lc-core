from locust import task, between, SequentialTaskSet
from locust.contrib.fasthttp import FastHttpUser
from locust import events
import json
import logging
import subprocess

@events.quitting.add_listener
def _(environment, **kw):
    totalStats = environment.stats.total
    if totalStats.fail_ratio > 0.20:
        logging.error("Test failed due to failure ratio " + totalStats.fail_ratio + " > 20%")
        environment.process_exit_code = 1
    elif totalStats.get_response_time_percentile(0.80) > 800:
        logging.error("Test failed due to 80th percentile response time > 800 ms")
        environment.process_exit_code = 1
    else:
        environment.process_exit_code = 0

class QuickStartUser(SequentialTaskSet):
    wait_time = between(0.1,0.2)

    @task
    def captcha(self):
        captcha_params = {"level":"debug","media":"image/png","input_type":"text", "size":"350x100"}

        with self.client.post(path="/v2/captcha", json=captcha_params, name="/captcha", catch_response = True) as resp:
          if resp.status_code != 200:
            resp.failure("Status was not 200: " + resp.text)
          captchaJson = resp.json()
          uuid = captchaJson.get("id")
          if not uuid:
            resp.failure("uuid not returned on /captcha endpoint: " + resp.text)

        with self.client.get(path="/v2/media?id=%s" % uuid, name="/media", stream=True, catch_response = True) as resp:
          if resp.status_code != 200:
            resp.failure("Status was not 200: " + resp.text)

          media = resp.content

        ocrAnswer = self.solve(uuid, media)

        answerBody = {"answer": ocrAnswer,"id": uuid}
        with self.client.post(path='/v2/answer', json=answerBody, name="/answer", catch_response=True) as resp:
          if resp.status_code != 200:
              resp.failure("Status was not 200: " + resp.text)
          else:
            if resp.json().get("result") != "True":
              resp.failure("Answer was not accepted: " + ocrAnswer)

    def solve(self, uuid, media):
       mediaFileName = "tests/test-%s.png" % uuid
       with open(mediaFileName, "wb") as f:
           f.write(media)
       #ocrResult = subprocess.Popen("gocr %s" % mediaFileName, shell=True, stdout=subprocess.PIPE)
       ocrResult = subprocess.Popen("tesseract %s stdout -l eng" % mediaFileName, shell=True, stdout=subprocess.PIPE)
       ocrAnswer = ocrResult.stdout.readlines()[0].strip().decode()
       return ocrAnswer



class User(FastHttpUser):
    wait_time = between(0.1,0.2)
    tasks = [QuickStartUser]
    host = "http://localhost:8888"
