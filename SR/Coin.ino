//
// -------- Coin acceptor --------
// Ported from the standalone Arduino/uni_arduino_project sketch. Detects a coin
// passing the analog sensor using a slow baseline (EMA) and a fast dip below it.
// Runs non-blocking from Task1code(); reports the count over BLE (see SR.ino).
//

// Tuning constants (same values the standalone sketch used).
static const float COIN_ALPHA          = 0.01f; // baseline smoothing factor
static const int   COIN_ENTER_THRESH   = 120;   // dip below baseline to count a coin
static const int   COIN_EXIT_THRESH    = 50;    // dip must fall back under this to re-arm
static const unsigned long COIN_LOCKOUT = 300;  // ms ignored after a coin, debounce

static float coinBaseline = 0.0f;
static bool  coinActive = false;
static unsigned long coinLastMs = 0;

void initCoin()
{
  pinMode(COIN_SENSE, INPUT);

  // Power/indicator pin: driven HIGH once and left on (old sketch's sourcePin).
  pinMode(COIN_SENSOR_PWR, OUTPUT);
  digitalWrite(COIN_SENSOR_PWR, HIGH);

  // Per-coin indicator LED, off to start (old sketch's alertPin).
  pinMode(COIN_LED, OUTPUT);
  digitalWrite(COIN_LED, LOW);

  delay(200); // let the sensor settle before reading the baseline

  // Seed the baseline with a real reading (the standalone sketch did this at
  // global scope, before the ADC was configured, which gave a bogus baseline).
  coinBaseline = analogRead(COIN_SENSE);
}

// Call frequently from the main task loop. Increments coinCount and notifies
// the app when a new coin is detected.
void pollCoin()
{
  int reading = analogRead(COIN_SENSE);

  // Slowly track the resting level so drift/temperature don't cause false counts.
  coinBaseline += COIN_ALPHA * (reading - coinBaseline);
  int diff = (int)coinBaseline - reading;

  if (millis() - coinLastMs < COIN_LOCKOUT) return;

  if (!coinActive && diff > COIN_ENTER_THRESH)
  {
    coinActive = true;
    coinLastMs = millis();

    digitalWrite(COIN_LED, HIGH); // light the indicator while the coin passes

    if (coinCount < 255) coinCount++;
    Serial.printf("Coin inserted, count %u\n", coinCount);
    notifyCoinCount();
  }

  if (coinActive && diff < COIN_EXIT_THRESH)
  {
    coinActive = false;
    digitalWrite(COIN_LED, LOW); // coin has left the sensor
  }
}
