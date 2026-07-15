

//gibt die Batteriespannung in mV zurück, sollte zwischen 14,400V (low) und 16,800V (Full) liegen
uint16_t ReadBatteryVoltage()
{
  uint16_t raw = 0;
  const int samples = 4;

  const uint16_t rawAt16800 = 2965;
  const uint16_t rawAt14400 = 2509;

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



void hard_mute_speaker(){
    Attenuator.mute();
    OutputPin.setAudioSourceDAC();
    OutputPin.MuteDac();
    OutputPin.updateOutputs();
}

void unmute_speaker(){
  if(Audiosource == 0)
  {
    set_source_DAC();
  }
  else
  {  
    set_source_Radio();
  }


}

void set_source_DAC()
{
  Audiosource = 0;
  sourceChar->setValue(&Audiosource,1);

  Attenuator.mute();
  OutputPin.setAudioSourceDAC();
  OutputPin.UnmuteDac();
  OutputPin.updateOutputs();
  delay(1);
  Attenuator.unmute();


}

void set_source_Radio()
{
  Audiosource = 1;
  sourceChar->setValue(&Audiosource,1);
  Attenuator.mute();
  delay(50);
  OutputPin.setAudioSourceRadio();
  OutputPin.MuteDac();
  OutputPin.updateOutputs();
  delay(50);
  Attenuator.unmute();

}


uint8_t batteryPercent(uint16_t mv)
{
    if (mv <= 14400) return 0;
    if (mv >= 16800) return 100;

    return (mv - 13200) * 100 / (16800 - 13200);
}

void shutdown()
{
  OutputPin.Shutdown();
  hard_mute_speaker();
  
  //OutputPin.updateOutputs();

}
