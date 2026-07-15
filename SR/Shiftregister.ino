void Shiftregister::MuteDac()
{
  this->PinRegister = this->PinRegister & ~(1 << this->Mutepin);
}

void Shiftregister::UnmuteDac()
{
  this->PinRegister = this->PinRegister | 1 << this->Mutepin;
}

void Shiftregister::setAudioSourceRadio()
{
  this->PinRegister = this->PinRegister | 1 << (this->Relaypin);
}

void Shiftregister::setAudioSourceDAC()
{
  this->PinRegister = this->PinRegister & ~(1 << (this->Relaypin));
}

void Shiftregister::Shutdown()
{
  this->PinRegister = this->PinRegister | 1 << (this->offpin);
}

void Shiftregister::HBridge_1()
{ 
  this->PinRegister = this->PinRegister & ~(1 << (this->H2_pin)); // 2 LOW
  this->PinRegister = this->PinRegister | 1 << (this->H1_pin);  //1 HIGH
}

void Shiftregister::HBridge_2()
{ 
  this->PinRegister = this->PinRegister & ~(1 << (this->H1_pin)); // 1 LOW
  this->PinRegister = this->PinRegister | 1 << (this->H2_pin);  //2 HIGH
}

void Shiftregister::HBridge_off()
{
  this->PinRegister = this->PinRegister & ~(1 << (this->H1_pin)); // 1 LOW
  this->PinRegister = this->PinRegister & ~(1 << (this->H2_pin)); // 2 LOW
}


void Shiftregister::initSR()
{
  this->PinRegister = 0b10000000;
  this->updateOutputs();
}

void Shiftregister::setAll()
{
  this->PinRegister = 0b10000001;
  this->updateOutputs();
}

void Shiftregister::updateOutputs()
{
  this->shiftRegisterWrite();
}



//
// -------- SN74HC595 --------
//
void Shiftregister::shiftRegisterWrite()
{
  // Alle CS deaktivieren
  digitalWrite(PIN_SD_CS, HIGH);
  digitalWrite(PIN_LM1971_CS, HIGH);

  // Ausgänge während des Schiebens nicht übernehmen
  digitalWrite(PIN_595_RCLK, LOW);
  delay(1);

  spi.beginTransaction(
      SPISettings(1000000, MSBFIRST, SPI_MODE0));

  spi.transfer(this->PinRegister);

  spi.endTransaction();

  // Daten auf Ausgänge übernehmen
  digitalWrite(PIN_595_RCLK, HIGH);
  digitalWrite(PIN_595_RCLK, LOW);
  delay(1);
  //Serial.printf("updated %u\n", this->PinRegister);
}