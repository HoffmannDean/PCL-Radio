

//gibt die Batteriespannung in mV zurück, sollte zwischen 14,400V (low) und 16,800V (Full) liegen
uint16_t ReadBatteryVoltage()
{
  uint16_t raw = 0;
  const int samples = 4;

  const uint16_t rawAt16800 = 3103;
  const uint16_t rawAt14400 = 2656;

  const int steigung = (16800 - 14400) / (rawAt16800 - rawAt14400);
  const int offset = 14400 - (steigung * rawAt14400);

  for(int i = 0; i < samples; i++)
  {
    raw += analogRead(BATTERY_SENSE);
    delay(1);
  }
  raw = raw / samples;

  uint16_t Voltage = (steigung * raw) + offset;

  Serial.printf("raw %u, Voltage %u\n", raw, Voltage);
  return Voltage;
}