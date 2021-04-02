from locust import task, between, SequentialTaskSet
from locust.contrib.fasthttp import FastHttpUser
from locust import events
import json
import logging

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
    wait_time = between(0.1,1)

    @task
    def captcha(self):
        # TODO: Iterate over parameters for a more comprehensive test
        captcha_params = {"level":"easy","media":"image/png","input_type":"text"}

        resp = self.client.post(path="/v1/captcha", json=captcha_params, name="/captcha")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.CAPTCHA-------------------\n\n")
        
        uuid = json.loads(resp.text).get("id")
        answerBody = {"answer": "qwer123","id": uuid}

        resp = self.client.get(path="/v1/media?id=%s" % uuid, name="/media")
        if resp.status_code != 200:
            print("\nError on /media endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.MEDIA-------------------\n\n")

        resp = self.client.post(path='/v1/answer', json=answerBody, name="/answer")
        if resp.status_code != 200:
            print("\nError on /answer endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.ANSWER-------------------\n\n")


class User(FastHttpUser):
    wait_time = between(0.1,1)
    tasks = [QuickStartUser]
    host = "http://localhost:8888"
