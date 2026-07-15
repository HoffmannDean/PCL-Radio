//
// -------- Coin acceptor --------
// Ported from the standalone Arduino/uni_arduino_project sketch. Detects a coin
// passing the analog sensor using a slow baseline (EMA) and a fast dip below it.
// Runs non-blocking from Task1code(); reports the count over BLE (see SR.ino).
//

// Tuning constants (same values the standalone sketch used).
static const float COIN_ALPHA          = 0.01f; // baseline smoothing factor
static const int   COIN_ENTER_THRESH   = 700;   // dip below baseline to count a coin
static const int   COIN_EXIT_THRESH    = 300;    // dip must fall back under this to re-arm
static const unsigned long COIN_LOCKOUT = 1200;  // ms ignored after a coin, debounce

static float coinBaseline = 0.0f;
static bool  coinActive = false;
static unsigned long coinLastMs = 0;

void initCoin()
{
  pinMode(COIN_SENSE, INPUT);
  // global scope, before the ADC was configured, which gave a bogus baseline).
  coinBaseline = analogRead(COIN_SENSE);
}

// Call frequently from the main task loop. Increments coinCount and notifies
// the app when a new coin is detected.
void pollCoin()
{
  int reading = analogRead(COIN_SENSE);

  Serial.printf("coin reading: %i\n", reading);

  // Slowly track the resting level so drift/temperature don't cause false counts.
  if (!coinActive)
  {
      coinBaseline += COIN_ALPHA * (reading - coinBaseline);
  }
  int diff = (int)coinBaseline - reading;

  if (millis() - coinLastMs < COIN_LOCKOUT) return;

  if (!coinActive && diff > COIN_ENTER_THRESH)
  {
    coinActive = true;
    coinLastMs = millis();

    if (coinCount < 255) coinCount++;
    Serial.printf("Coin inserted, count %u\n", coinCount);
    notifyCoinCount();
  }

  if (coinActive && diff < COIN_EXIT_THRESH)
  {
    coinActive = false;
  }
}





















static const int period = 20;

void turnForDropout(int time, int dutyCycle) {

  unsigned long startTime = millis();

  const int period = 20; // PWM-Periode in ms

  int onTime  = period * dutyCycle / 100;
  int offTime = period - onTime;

  while (millis() < startTime + time) {


    OutputPin.HBridge_2();
    OutputPin.updateOutputs();

    delay(onTime);
    OutputPin.HBridge_off();
    OutputPin.updateOutputs();

    delay(offTime);
  }
  OutputPin.HBridge_off();
  OutputPin.updateOutputs();
}


bool checkDetection(void){
  int value = analogRead(LDR_PIN);
  Serial.printf("Experte Photoresistor: %i\n",value);

  if(analogRead(LDR_PIN) > 3000) return true;
  else return false;
}


void turnBackIn(int dutyCycle) {
  int onTime  = period * dutyCycle / 100;
  int offTime = period - onTime;
  while(true){
    if(checkDetection()){
      OutputPin.HBridge_off();
      OutputPin.updateOutputs();
      return;
    }

    OutputPin.HBridge_1();
    OutputPin.updateOutputs();


    delay(onTime);

    OutputPin.HBridge_off();
    OutputPin.updateOutputs();

    delay(offTime);
  }
}