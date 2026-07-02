#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

const int sensorPin = 34;
const int sourcePin = 4;
const int alertPin  = 2;
int baseline = 0;
const int dropThreshold = 300;

int counter = 0;
bool disrupted = false;   // tracks current state for edge detection

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32-Test");
  pinMode(sourcePin, OUTPUT);
  pinMode(alertPin, OUTPUT);
  digitalWrite(sourcePin, HIGH);
  delay(200);
  baseline = analogRead(sensorPin);
  Serial.printf("Baseline light level: %d\n", baseline);
}

void loop() {
  int value = analogRead(sensorPin);
  bool nowDark = (baseline - value > dropThreshold);

  if (nowDark && !disrupted) {        // edge: clear -> blocked
    disrupted = true;
    digitalWrite(alertPin, HIGH);
    counter++;
    SerialBT.printf("COUNT:%d\n", counter);   // send to app
    Serial.printf("Disruption #%d\n", counter);
  } else if (!nowDark && disrupted) {  // edge: blocked -> clear
    disrupted = false;
    digitalWrite(alertPin, LOW);
  }

  delay(50);
}