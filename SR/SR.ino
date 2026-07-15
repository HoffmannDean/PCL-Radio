#include <SPI.h>
#include <SD.h>

#include "AudioTools.h"
#include "BluetoothA2DPSink.h"

#include <RotaryEncoder.h>
#include "defines.ino"

#include <WiFi.h>
#include "AudioTools/Communication/HTTP/ICYStream.h"
//#include "AudioTools/Disk/AudioSourceSD.h"
#include "AudioTools/AudioCodecs/CodecMP3Helix.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

const char* ssid = "Meddl";
const char* password = "kaschperletheater";
const char* url = "https://jazz.stream.laut.fm/jazz";




//BLE
#define BATTERY_SERVICE_UUID      "180F"
#define BATTERY_LEVEL_UUID        "2A19" //akkustand in Prozent

#define AMP_SERVICE_UUID          "8A5D0001-1111-2222-3333-123456789ABC"
#define SOURCE_CHARACTERISTIC_UUID "8A5D0002-1111-2222-3333-123456789ABC"  //Quelle, uint8, 0 für DAC / Bluetoothaudio, 1 für analogradio
#define VOLUME_CHARACTERISTIC_UUID "8A5D0003-1111-2222-3333-123456789ABC"  //abschwächung für den lautsprecher, uint8, -0db bis -64db
#define SHUTDOWN_CHARACTERISTIC_UUID "8A5D0004-1111-2222-3333-123456789ABC" //uint8, schreiben von "1" bewirkt shutdown des radios

// Casino / coin service. CoinCount notifies the app on every change; the app
// writes Dispense (number of coins) to pay out a win.
#define GAME_SERVICE_UUID          "8A5D0010-1111-2222-3333-123456789ABC"   
#define COINCOUNT_CHARACTERISTIC_UUID "8A5D0011-1111-2222-3333-123456789ABC"  //anzahl münzen im radio auslesen
#define DISPENSE_CHARACTERISTIC_UUID  "8A5D0012-1111-2222-3333-123456789ABC"  //uint8 schreiben wie viele münzen ausgeworfen werden sollen.

BLEService* batteryService;
BLEService* gameService;
BLEService* ampService;

BLECharacteristic* coinCountChar;
BLECharacteristic* dispenseChar;

BLECharacteristic* batteryChar;
BLECharacteristic* sourceChar;
BLECharacteristic* volumeChar;
BLECharacteristic* shutdownChar;

BLEAdvertising* advertising;
BLEServer* server;

// 0 DAC, 1 DAC
uint8_t Audiosource = 0;
uint8_t ble_attenuation = 64;
uint8_t ble_shutdown = 0;


//int source_radio = 0;

//globaler Mute
//bool mute = false;

uint16_t battery_voltage = 50000;
uint8_t batteryLevel = 100;

uint8_t coinCount = 0;
uint8_t pendingDispense = 0;



TaskHandle_t Task1;



RotaryEncoder *encoder;
SPIClass spi(VSPI);

I2SStream i2s;
BluetoothA2DPSink a2dp_sink(i2s);

//AudioSourceSD source(PATH, EXT);
//MP3DecoderHelix decoder;
//AudioPlayer player(source, i2s, decoder);

//ICYStream icystream;
//EncodedAudioStream mp3decode( new MP3DecoderHelix());
//StreamCopy copier(mp3decode, icystream);


void printMetaData(MetaDataType type, const char* str, int len) {
  Serial.printf("%s: %s\n", toStr(type), str);
}


//monosignal mischen
void audio_data_callback(const uint8_t *data, uint32_t len)
{
    int16_t *samples = (int16_t *)data;

    // Stereo: L,R,L,R,...
    for (uint32_t i = 0; i < len / 2; i += 2) {
        int16_t mono = ((int32_t)samples[i] + samples[i + 1]) / 2;

        // beide Kanäle auf Mono setzen
        samples[i]     = mono;
        samples[i + 1] = mono;
    }
}



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

    bool Mute = false; // nicht beschreiben! ist public um im callback ausgelesen zu werden

  private:
    void lm1971Write(uint16_t value);
    uint16_t Attenuation = 20;
    
};



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
    void HBridge_1();
    void HBridge_2();
    void HBridge_off();

  private:
    // 0bHGFEDCBA , soft-off = G, Mute = H, Relay = A
    uint8_t PinRegister;
    void shiftRegisterWrite();

    uint8_t Mutepin = 7;
    uint8_t Relaypin = 0;
    uint8_t offpin = 6;
    uint8_t H1_pin = 4;
    uint8_t H2_pin = 5;
};

LM1971 Attenuator;
Shiftregister OutputPin;


class SourceCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {

        uint8_t value =
            pCharacteristic->getData()[0];

        switch(value)
        {
            case 0:
              Serial.println("DAC");
                set_source_DAC();
                break;

            case 1:
             Serial.println("RADIO");
                set_source_Radio();
                break;
            case 2:
                hard_mute_speaker();
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

class ShutdownCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {

    uint8_t value = pCharacteristic->getData()[0];
    if(value == 1)
    {
      shutdown();
    }
  }
};

// App writes the number of coins to pay out (a casino win writes the whole
// jackpot). dispenseCoins() runs the motor, drains coinCount and notifies.
class DispenseCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
    uint8_t value = pCharacteristic->getData()[0];
    pendingDispense += value;
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
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_READ
      );

  shutdownChar = 
      ampService->createCharacteristic(
        SHUTDOWN_CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_READ
      );

  
  sourceChar->setValue(&Audiosource,1);
  volumeChar->setValue(&ble_attenuation,1);
  shutdownChar->setValue(&ble_shutdown,1);

  sourceChar->setCallbacks(
      new SourceCallback()
  );

  shutdownChar->setCallbacks(
    new ShutdownCallback()
  );

  volumeChar->setCallbacks(new VolumeCallback());






  
  sourceChar->addDescriptor(new BLE2902());
  volumeChar->addDescriptor(new BLE2902());
  shutdownChar->addDescriptor(new BLE2902());

  ampService->start();
  delay(10);
  advertising->addServiceUUID(AMP_SERVICE_UUID);

  batteryService = server->createService(BATTERY_SERVICE_UUID);
  batteryChar = batteryService->createCharacteristic(
      BATTERY_LEVEL_UUID,
      BLECharacteristic::PROPERTY_READ |
      BLECharacteristic::PROPERTY_NOTIFY
  );

  batteryChar->addDescriptor(new BLE2902());
  batteryChar->setValue(&batteryLevel, 1);
  batteryService->start();
  delay(10);
  advertising->addServiceUUID(BATTERY_SERVICE_UUID);

  // --- Casino / coin service ---
  gameService = server->createService(GAME_SERVICE_UUID);

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
  delay(10);
  advertising->addServiceUUID(GAME_SERVICE_UUID);
}





//
// -------- SD-Karte --------
//
bool initSD()
{
  digitalWrite(PIN_LM1971_CS, HIGH);

 return SD.begin(PIN_SD_CS, spi);
 //return 0;
}

void initWifi()
{

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void callbackMetadata(MetaDataType type, const char* str, int len) {
  Serial.printf("%s: %s\n", toStr(type), str);
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
  pinMode(PHONO_PIN, INPUT_PULLUP);
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
  delay(1);
  OutputPin.setAll();
  delay(10);

  initCoin();

  //initWifi();

  encoder = new RotaryEncoder(ENCODER_A, ENCODER_B, RotaryEncoder::LatchMode::FOUR3);
  attachInterrupt(digitalPinToInterrupt(ENCODER_A), checkPosition, CHANGE);
  attachInterrupt(digitalPinToInterrupt(ENCODER_B), checkPosition, CHANGE);

  



  auto config = i2s.defaultConfig();
  config.pin_bck = BCLK_PIN;
  config.pin_ws = LRCK_PIN;
  config.pin_data = DIN_PIN;  
  i2s.begin(config);

  //mp3decode.begin();
  //icystream.begin(url);
  //icystream.setMetadataCallback(callbackMetadata);



  //für BLE
  BLEDevice::init("MyMusic");
  delay(10);
  server = BLEDevice::createServer();
  delay(10);
  advertising = BLEDevice::getAdvertising();
  delay(10);
  startService();
  delay(10);
  advertising->start();
  delay(10);
  a2dp_sink.set_stream_reader(audio_data_callback, true);
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

  if(digitalRead(PHONO_PIN))
  {
      set_source_DAC();
  }
  else
  {
      set_source_Radio();
  }


  // Test: Relais/LEDs am 74HC595
  //shiftRegisterWrite(0x55);

  // Test: LM1971
  delay(10);
  Attenuator.setAttenuation(35);
  delay(10);
  Attenuator.unmute();
  delay(10);
  Attenuator.setAttenuation(35);
  delay(50);
  

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

  
  delay(25);

  int value = analogRead(LDR_PIN);
  Serial.printf("Experte Photoresistor: %i\n",value);
  //delay(10);

  if(pendingDispense > 0)
  {
    if(coinCount > 0)
    {
      turnForDropout(700, 20);
      delay(1000);
      turnBackIn(17);
      delay(250);
      pendingDispense--;
      coinCount--;
      coinCountChar->setValue(&coinCount,1);
      coinCountChar->notify();
    }
    else
    {
      pendingDispense = 0;
    }
  }

  


}

void Task1code( void * pvParameters ){
  int pos = 0;
  unsigned long BTN_PRESS_TIME = millis();
  const unsigned long TURN_OFF_PRESS_TIME = 2500;
  unsigned long last_battery_read = millis();
  bool lastPhono = false;

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
        hard_mute_speaker();
        OutputPin.Shutdown();
        OutputPin.updateOutputs();
        BTN_PRESS_TIME += 20000;
      }
    }
    else
    {
      //release
      if(BTN_PRESS_TIME > 750 && BTN_PRESS_TIME < 2000)
      {

          //kurzes Drücken für Mute_toggle
        if(Attenuator.isMute())
        {
          unmute_speaker();
        }
        else
        {
          Attenuator.mute();
        }
      }


      BTN_PRESS_TIME = millis();
    }




    if(last_battery_read + 10000 < millis())
    {
      battery_voltage = ReadBatteryVoltage();
      batteryLevel = batteryPercent(battery_voltage);
      batteryChar->setValue(&batteryLevel, 1);
      batteryChar->notify();



      //low bat shutdown
      if(battery_voltage < 14400)
      {
        shutdown();
      }

      last_battery_read = millis();
    }
    bool phono = digitalRead(PHONO_PIN);
    if(phono != lastPhono)
    {
      if(phono)
      {
        set_source_DAC();
      }
      else
      {
        set_source_Radio();
      }
      delay(10);
      lastPhono = phono;
    }

    //copier.copy();
    pollCoin();
    delay(10);
  }
}