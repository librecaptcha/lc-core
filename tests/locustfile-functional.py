from locust import task, between, SequentialTaskSet
from locust.contrib.fasthttp import FastHttpUser
from locust import events
import json
import logging
import subprocess

@events.quitting.add_listener
def _(environment, **kw):
    if environment.stats.total.fail_ratio > 0.02:
        logging.error("Test failed due to failure ratio > 2%")
        environment.process_exit_code = 1
    elif environment.stats.total.avg_response_time > 300:
        logging.error("Test failed due to average response time ratio > 300 ms")
        environment.process_exit_code = 1
    elif environment.stats.total.get_response_time_percentile(0.95) > 800:
        logging.error("Test failed due to 95th percentile response time > 800 ms")
        environment.process_exit_code = 1
    else:
        environment.process_exit_code = 0

class QuickStartUser(SequentialTaskSet):
    wait_time = between(0.1,0.2)

    @task
    def captcha(self):
        # TODO: Iterate over parameters for a more comprehensive test
        captcha_params = {"level":"debug","media":"image/png","input_type":"text"}

        resp = self.client.post(path="/v1/captcha", json=captcha_params, name="/captcha")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.CAPTCHA-------------------\n\n")
        
        uuid = resp.json().get("id")

        resp = self.client.get(path="/v1/media?id=%s" % uuid, name="/media", stream=True)
        if resp.status_code != 200:
            print("\nError on /media endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.MEDIA-------------------\n\n")

        media = resp.content
        mediaFileName = "tests/test-%s.png" % uuid
        with open(mediaFileName, "wb") as f:
            f.write(media)
        ocrResult = subprocess.Popen("gocr %s" % mediaFileName, shell=True, stdout=subprocess.PIPE)
        ocrAnswer = ocrResult.stdout.readlines()[0].strip().decode()

        answerBody = {"answer": ocrAnswer,"id": uuid}
        with self.client.post(path='/v1/answer', json=answerBody, name="/answer", catch_response=True) as resp:
          if resp.status_code != 200:
              print("\nError on /answer endpoint: ")
              print(resp)
              print(resp.text)
              print("----------------END.ANSWER-------------------\n\n")
          else:
            if resp.json().get("result") != "True":
              resp.failure("Answer was not accepted")


class User(FastHttpUser):
    wait_time = between(0.1,0.2)
    tasks = [QuickStartUser]
    host = "http://localhost:8888"
