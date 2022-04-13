set -ex

python3 -m venv testEnv
source ./testEnv/bin/activate
pip install locust
mkdir -p data/
java -jar target/scala-3.1.2/LibreCaptcha.jar &
JAVA_PID=$!
sleep 4

locust --only-summary --headless -u 300 -r 100 --run-time 4m --stop-timeout 30 -f tests/locustfile.py
status=$?

if [ $status != 0 ]; then
  exit $status
fi

kill $JAVA_PID
sleep 4

echo Run functional test
cp data/config.json data/config.json.bak
cp tests/debug-config.json data/config.json

java -jar target/scala-3.1.2/LibreCaptcha.jar &
JAVA_PID=$!
sleep 4

locust --only-summary --headless -u 1 -r 1 --run-time 1m --stop-timeout 30 -f tests/locustfile-functional.py
status=$?
mv data/config.json.bak data/config.json

kill $JAVA_PID
exit $status
