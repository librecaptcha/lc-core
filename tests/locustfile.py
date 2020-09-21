from locust import task, between, SequentialTaskSet
from locust.contrib.fasthttp import FastHttpUser
import json
import uuid

class QuickStartUser(SequentialTaskSet):
    wait_time = between(0.1,1)

    captcha_params = {"level":"some","media":"some","input_type":"some"}
    answerBody = {"answer": "qwer123"}

    @task
    def captcha(self):
        resp = self.client.post(path="/v1/captcha", json=self.captcha_params, name="/captcha")
        if resp.status_code != 200:
            print("\nError on /captcha endpoint: ")
            print(resp)
            print(resp.text)
            print("----------------END.C-------------------\n\n")
        self.answerBody["id"] = json.loads(resp.text).get("id")

    @task
    def media(self):
        resp = self.client.get(path="/v1/media?id=%s" % self.answerBody.get("id"), name="/media")
        if resp.status_code != 200:
            print("\nError on /media endpoint: ")
            print(resp)
            print(resp.text)
            print("-----------------END.M-------------------\n\n")

    @task
    def answer(self):
        resp = self.client.post(path='/v1/answer', json=self.answerBody, name="/answer")
        if resp.status_code != 200:
            print("\nError on /answer endpoint: ")
            print(resp)
            print(resp.text)
            print("-------------------END.A---------------\n\n")


class User(FastHttpUser):
    wait_time = between(0.1,1)
    tasks = [QuickStartUser]
    host = "http://localhost:8888"
