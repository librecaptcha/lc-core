set -ex

python3 -m venv testEnv
source ./testEnv/bin/activate
pip install locust
java -jar target/scala-2.13/LibreCaptcha.jar &
JAVA_PID=$!
sleep 4

locust --headless -u 300 -r 100 --run-time 4m --stop-timeout 30 -f tests/locustfile.py
status=$?

kill $JAVA_PID

exit $status
