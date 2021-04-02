python3 -m venv testEnv
source ./testEnv/activate
pip install locust
java -jar target/scala-2.13/LibreCaptcha.jar &
JAVA_PID=$!
sleep 4

locust --headless -u 1000 -r 100 -f tests/locustfile.py

kill $JAVA_PID
