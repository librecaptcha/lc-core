import http.client
import json

conn = http.client.HTTPConnection('localhost', 8888)
conn.request("GET", "/v1/token?email=test")
response = conn.getresponse()
responseStr = response.read()
user = json.loads(responseStr)

params = """{
"level": "medium",
"media": "image/png",
"input_type": "text"
}"""

conn.request("POST", "/v1/captcha", body=params, headers={'access-token': user["token"]})

response = conn.getresponse()

if response:
    responseStr = response.read()
    captcha = json.loads(responseStr)
    print(captcha)
    captchaId = captcha["id"]
    reply = {"answer": "xyz", "id" : captchaId}
    conn.request("POST", "/v1/answer", json.dumps(reply))
    response = conn.getresponse()
    if response:
        responseStr = response.read()
        print(responseStr)


