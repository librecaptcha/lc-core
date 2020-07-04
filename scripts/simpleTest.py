import http.client
import json

conn = http.client.HTTPConnection('localhost', 8888)
conn.request("GET", "/v1/token?email=test")
response = conn.getresponse()
responseStr = response.read()
user = json.loads(responseStr)
token = user["token"]

params = """{
"level": "medium",
"media": "image/png",
"input_type": "text"
}"""

def getCaptcha():
    conn.request("POST", "/v1/captcha", body=params, headers={'access-token': user["token"]})
    response = conn.getresponse()

    if response:
        responseStr = response.read()
        return json.loads(responseStr)

def postAnswer(captchaId, ans):
    reply = {"answer": ans, "id" : captchaId}
    conn.request("POST", "/v1/answer", json.dumps(reply))
    response = conn.getresponse()
    if response:
        return response.read()
        print(responseStr)


for i in range(0, 10000):
    captcha = getCaptcha()
    #print(captcha)
    captchaId = captcha["id"]
    print(i, postAnswer(captchaId, "xyz"))
