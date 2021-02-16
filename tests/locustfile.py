from locust import task, between, SequentialTaskSet
from locust.contrib.fasthttp import FastHttpUser
import json

class QuickStartUser(SequentialTaskSet):
    wait_time = between(0.1,1)

    @task
    def captcha(self):
        captcha_params = {"level":"some","media":"some","input_type":"some"}

        resp = self.client.post(path="/v1/captcha", json=captcha_params, name="/captcha")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.C-------------------\n\n")
        
        uuid = json.loads(resp.text).get("id")
        answerBody = {"answer": "qwer123","id": uuid}

        resp = self.client.get(path="/v1/media?id=%s" % uuid, name="/media")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.C-------------------\n\n")

        resp = self.client.post(path='/v1/answer', json=answerBody, name="/answer")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.C-------------------\n\n")


class User(FastHttpUser):
    wait_time = between(0.1,1)
    tasks = [QuickStartUser]
    host = "http://localhost:8888"
