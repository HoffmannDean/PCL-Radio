#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

const int sensorPin = 34;
const int sourcePin = 4;
const int alertPin  = 2;

float baseline = analogRead(sensorPin);
bool active = false;
unsigned long lastCoin = 0;
int counter = 0;

const float alpha = 0.01;
const int enterThreshold = 120;
const int exitThreshold = 50;
const int lockout = 300;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32-Test");
  pinMode(sourcePin, OUTPUT);
  pinMode(alertPin, OUTPUT);
  digitalWrite(sourcePin, HIGH);
  delay(200);
}

void loop() {
  int reading = analogRead(sensorPin);

  baseline += alpha * (reading - baseline);
  int diff = baseline - reading;

  Serial.print("Baseline: ");
  Serial.println(baseline);

  Serial.print("Reading: ");
  Serial.println(reading);

  if (millis() - lastCoin < lockout) return;

  if (!active && diff > enterThreshold) {
      active = true;
      lastCoin = millis();

      // coin counted
      counter++;
      digitalWrite(alertPin, HIGH);

      SerialBT.printf("COUNT:%d\n", counter);   // send to app
      Serial.print("Count: ");
      Serial.println(counter);
  }

  if (active && diff < exitThreshold) {
      active = false;
      digitalWrite(alertPin, LOW);
  }
  
  delay(50);
}
