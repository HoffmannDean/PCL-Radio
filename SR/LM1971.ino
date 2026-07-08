
int LM1971::getAttenuation() 
{
  if(this->Attenuation > 62)
  {
    return 96;
  }
  return this->Attenuation;
}

void LM1971::setAttenuation(int attenuation) 
{
  this->Attenuation = constrain(attenuation, 0, 64);
  if(this->Mute == false)
  {
    this->lm1971Write(this->Attenuation);
  }
}

void LM1971::mute()
{
  this->Mute = true;
  this->lm1971Write(63);
}

void LM1971::unmute()
{
  this->Mute = false;
  this->lm1971Write(this->Attenuation);
}

bool LM1971::isMute()
{
  return this->Mute;
}

bool LM1971::volumeTickUp() 
{
  if(this->Attenuation > 0)
  {
    this->Attenuation--;
    if(this->Mute == false)
    {
      this->lm1971Write(this->Attenuation);
    }
    return true;
  }
  return false;
}

bool LM1971::volumeTickDown()
{
  if(this->Attenuation < 63)
  {
    this->Attenuation++;
    if(this->Mute == false)
    {
      this->lm1971Write(this->Attenuation);
    }
    return true;
  }
  return false;
}



//
// -------- LM1971 - Audio Attenuator --------
//
void LM1971::lm1971Write(uint16_t value)
{
  // Andere Teilnehmer deaktivieren
  digitalWrite(PIN_SD_CS, HIGH);
  digitalWrite(PIN_LM1971_CS, LOW);

  spi.beginTransaction(SPISettings(100000, MSBFIRST, SPI_MODE1));
  spi.transfer16(value);
  spi.endTransaction();

  digitalWrite(PIN_LM1971_CS, HIGH);
}