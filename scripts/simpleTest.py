import http.client
import json
import subprocess
import os

tempDir = os.getenv('XDG_RUNTIME_DIR', '.')
conn = http.client.HTTPConnection('localhost', 8888)

params = """{
"level": "debug",
"media": "image/png",
"input_type": "text"
}"""

def getCaptcha():
    conn.request("POST", "/v1/captcha", body=params)
    response = conn.getresponse()

    if response:
        responseStr = response.read()
        return json.loads(responseStr)

def getAndSolve(idStr):
    conn.request("GET", "/v1/media?id=" + idStr)
    response = conn.getresponse()

    if response:
        responseBytes = response.read()
        fileName = tempDir + "/captcha.png"
        with open(fileName, "wb") as f:
            f.write(responseBytes)
        ocrResult = subprocess.Popen("gocr " + fileName, shell=True, stdout=subprocess.PIPE)
        ocrAnswer = ocrResult.stdout.readlines()[0].strip().decode()
        return ocrAnswer

def postAnswer(captchaId, ans):
    reply = {"answer": ans, "id" : captchaId}
    conn.request("POST", "/v1/answer", json.dumps(reply))
    response = conn.getresponse()
    if response:
        return response.read()
        print(responseStr)


for i in range(0, 10000):
    captcha = getCaptcha()
    captchaId = captcha["id"]
    ans = getAndSolve(captchaId)
    print(i, postAnswer(captchaId, ans))
