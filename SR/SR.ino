#include <SPI.h>
#include <SD.h>

#include "AudioTools.h"
#include "BluetoothA2DPSink.h"

#include <RotaryEncoder.h>
#include "defines.ino"


#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

//BLE
#define BATTERY_SERVICE_UUID      "180F"
#define BATTERY_LEVEL_UUID        "2A19"

#define AMP_SERVICE_UUID          "8A5D0001-1111-2222-3333-123456789ABC"
#define SOURCE_CHARACTERISTIC_UUID "8A5D0002-1111-2222-3333-123456789ABC"
#define VOLUME_CHARACTERISTIC_UUID "8A5D0003-1111-2222-3333-123456789ABC"

// Casino / coin service. CoinCount notifies the app on every change; the app
// writes Dispense (number of coins) to pay out a win.
#define GAME_SERVICE_UUID          "8A5D0010-1111-2222-3333-123456789ABC"
#define COINCOUNT_CHARACTERISTIC_UUID "8A5D0011-1111-2222-3333-123456789ABC"
#define DISPENSE_CHARACTERISTIC_UUID  "8A5D0012-1111-2222-3333-123456789ABC"

BLECharacteristic* batteryChar;
BLECharacteristic* sourceChar;
BLECharacteristic* volumeChar;
BLECharacteristic* coinCountChar;
BLECharacteristic* dispenseChar;
BLEServer* server;

// Jackpot: coins inserted since the last payout. The ESP owns this value;
// Coin.ino increments it, Dispenser.ino decrements it, and notifyCoinCount()
// pushes it to the app. uint8_t is plenty (0..255).
uint8_t coinCount = 0;

// Coins requested for payout. The BLE write callback only bumps this; the
// actual (blocking) motor run happens in Task1code so it can't stall the BLE
// stack and drop the connection.
volatile uint8_t pendingDispense = 0;





TaskHandle_t Task1;



RotaryEncoder *encoder;
SPIClass spi(VSPI);

I2SStream i2s;
BluetoothA2DPSink a2dp_sink(i2s);



void IRAM_ATTR checkPosition()
{
  encoder->tick(); // just call tick() to check the encoder state.
}

class LM1971 {
  public:
    int getAttenuation(); //von 0dB bis 62dB Abschwächung. 63 ist wie Mute. 
    void setAttenuation(int attenuation);
    void mute();
    void unmute();
    bool isMute();
    bool volumeTickUp();  //wahr wenn die Lautstärke erhöht werden konnte
    bool volumeTickDown(); //wahr wenn die Lautstärke verringert werden konnte

  private:
    void lm1971Write(uint16_t value);
    uint16_t Attenuation = 10;
    bool Mute = false;
};

LM1971 Attenuator;

class Shiftregister {
  public:
    void MuteDac();
    void UnmuteDac();
    void setAudioSourceRadio();
    void setAudioSourceDAC();
    void Shutdown();
    void updateOutputs();
    void initSR();
    void setAll();

  private:
    // 0bHGFEDCBA , soft-off = G, Mute = H, Relay = A
    uint8_t PinRegister;
    void shiftRegisterWrite();

    uint8_t Mutepin = 7;
    uint8_t Relaypin = 0;
    uint8_t offpin = 6;
};

Shiftregister OutputPin;





class SourceCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {

        uint8_t value =
            pCharacteristic->getData()[0];

        switch(value)
        {
            case 0:
              Serial.println("DAC");
                Attenuator.mute();
                OutputPin.setAudioSourceDAC();
                OutputPin.UnmuteDac();
                OutputPin.updateOutputs();
                Attenuator.unmute();
                break;

            case 1:
             Serial.println("RADIO");
                Attenuator.mute();
                OutputPin.setAudioSourceRadio();
                OutputPin.MuteDac();
                OutputPin.updateOutputs();
                Attenuator.unmute();
                break;
            case 2:
                Attenuator.mute();
                OutputPin.setAudioSourceDAC();
                OutputPin.MuteDac();
                OutputPin.updateOutputs();
                break;
        }

    }

    
};


class VolumeCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {

    uint8_t value = pCharacteristic->getData()[0];
    Attenuator.setAttenuation(value);
  }
};


// App writes the number of coins to pay out (a casino win writes the whole
// jackpot). dispenseCoins() runs the motor, drains coinCount and notifies.
class DispenseCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
    uint8_t value = pCharacteristic->getData()[0];
    pendingDispense += value; // handled in Task1code, not here
  }
};


// Push the current jackpot to the app. Safe to call before BLE is up (the
// characteristic pointer is checked), so Coin.ino can call it freely.
void notifyCoinCount()
{
  if (coinCountChar == nullptr) return;
  coinCountChar->setValue(&coinCount, 1);
  coinCountChar->notify();
}


BLEService* ampService;
BLEAdvertising* advertising;
uint8_t Audiosource = 0;


void startService() 
{
  ampService = server->createService(AMP_SERVICE_UUID);

  sourceChar =
      ampService->createCharacteristic(
          SOURCE_CHARACTERISTIC_UUID,
          BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE
      );

  volumeChar = 
      ampService->createCharacteristic(
        VOLUME_CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_WRITE
      );

  
  sourceChar->setValue(&Audiosource,1);
  //volumeChar->setValue(Attenuator->getAttenuation());

  sourceChar->setCallbacks(
      new SourceCallback()
  );

  volumeChar->setCallbacks(new VolumeCallback());

  ampService->start();
  delay(100);
  advertising->addServiceUUID(AMP_SERVICE_UUID);

  // --- Casino / coin service ---
  BLEService* gameService = server->createService(GAME_SERVICE_UUID);

  coinCountChar =
      gameService->createCharacteristic(
          COINCOUNT_CHARACTERISTIC_UUID,
          BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_NOTIFY
      );
  coinCountChar->addDescriptor(new BLE2902()); // lets the app subscribe
  coinCountChar->setValue(&coinCount, 1);

  dispenseChar =
      gameService->createCharacteristic(
          DISPENSE_CHARACTERISTIC_UUID,
          BLECharacteristic::PROPERTY_WRITE
      );
  dispenseChar->setCallbacks(new DispenseCallback());

  gameService->start();
  delay(100);
  advertising->addServiceUUID(GAME_SERVICE_UUID);
}





//
// -------- SD-Karte --------
//
bool initSD()
{
  digitalWrite(PIN_LM1971_CS, HIGH);

  return SD.begin(PIN_SD_CS, spi);
}

void setup()
{

  Serial.begin(115200);

  pinMode(PIN_SD_CS, OUTPUT);
  pinMode(PIN_LM1971_CS, OUTPUT);
  pinMode(PIN_595_RCLK, OUTPUT);



  pinMode(ENCODER_A, INPUT_PULLUP);
  pinMode(ENCODER_B, INPUT_PULLUP);
  pinMode(ENCODER_BTN, INPUT);
  analogReadResolution(12);



  digitalWrite(PIN_SD_CS, HIGH);
  digitalWrite(PIN_LM1971_CS, HIGH);
  digitalWrite(PIN_595_RCLK, LOW);

    // SPI-Bus initialisieren
  spi.begin(
      PIN_SPI_SCK,
      PIN_SPI_MISO,
      PIN_SPI_MOSI,
      -1
  );


  //OutputPin.initSR();
  delay(10);
  OutputPin.setAll();
  delay(10);

  encoder = new RotaryEncoder(ENCODER_A, ENCODER_B, RotaryEncoder::LatchMode::FOUR3);
  attachInterrupt(digitalPinToInterrupt(ENCODER_A), checkPosition, CHANGE);
  attachInterrupt(digitalPinToInterrupt(ENCODER_B), checkPosition, CHANGE);

  



  auto config = i2s.defaultConfig();
  config.pin_bck = BCLK_PIN;
  config.pin_ws = LRCK_PIN;
  config.pin_data = DIN_PIN;  
  i2s.begin(config);



  //für BLE
  BLEDevice::init("MyMusic");
  delay(100);
  server = BLEDevice::createServer();
  delay(100);
  advertising = BLEDevice::getAdvertising();
  delay(100);
  startService();
  delay(100);
  advertising->start();
  delay(100);
  a2dp_sink.start("MyMusic");


  // SD initialisieren
  if (initSD())
  {
    Serial.println("SD Karte OK");
  }
  else
  {
    Serial.println("SD Karte Fehler");
  }

  // Test: Relais/LEDs am 74HC595
  //shiftRegisterWrite(0x55);

  // Test: LM1971
  delay(10);
  Attenuator.setAttenuation(25);
  delay(10);
  Attenuator.unmute();
  delay(10);
  Attenuator.setAttenuation(25);
  delay(50);
  

  // Coin acceptor + dispenser (merged coin-counter functionality)
  initCoin();
  initDispenser();

  Serial.println("create Task");

    xTaskCreatePinnedToCore(
                    Task1code,   /* Task function. */
                    "Task1",     /* name of task. */
                    10000,       /* Stack size of task */
                    NULL,        /* parameter of the task */
                    1,           /* priority of the task */
                    &Task1,      /* Task handle to keep track of created task */
                    0);          /* pin task to core 0 */           
  
}



void loop()
{


}

void Task1code( void * pvParameters ){
  int pos = 0;
  unsigned long BTN_PRESS_TIME = millis();
  const unsigned long TURN_OFF_PRESS_TIME = 2500;

  for(;;){

    //Encoder Lautstärke
    int newPos = encoder->getPosition();
    if (pos != newPos) {
      if(newPos > pos)
      {
        Attenuator.volumeTickDown();
        Serial.println("leiser");
      } else {
        Attenuator.volumeTickUp();
        Serial.println("lauter");
      }
      delay(10);
      pos = newPos;
    }

    // Coin acceptor: detect inserted coins and notify the app.
    pollCoin();

    // Coin dispenser: run any coins the app requested paying out.
    if (pendingDispense > 0)
    {
      uint8_t n = pendingDispense;
      pendingDispense = 0;
      dispenseCoins(n);
    }

    //OutputPin.initSR();
    //delay(1000);
    //OutputPin.setAll();
    //delay(1000);

    //Serial.println(ReadBatteryVoltage());
    //ReadBatteryVoltage();


    //Shutdown Logik
    if(!digitalRead(ENCODER_BTN))
    {
      //Pressed 
      if(BTN_PRESS_TIME + TURN_OFF_PRESS_TIME < millis())
      {
        //Wird ausgeführt wenn der Button zuletzt vor TURN_OFF_PRESS_TIME released war
        OutputPin.Shutdown();
        OutputPin.updateOutputs();
        BTN_PRESS_TIME += 20000;
      }
    }
    else
    {
      //release
      BTN_PRESS_TIME = millis();
    }
    delay(25);
  }
}