//
// -------- Coin dispenser (H-bridge motor) --------
// STUB: the payout mechanism is not built yet. A win rotates the motor ~90 deg
// to release one coin, then rotates back. Timing/logic here is a placeholder;
// replace the delays with real values (or an encoder/limit switch) once the
// hardware exists. The BLE Dispense characteristic (SR.ino) calls dispenseCoins().
//

// How long to drive the motor to reach ~90 deg. Pure guess until the gearing is
// known -- measure and adjust on the real mechanism.
static const unsigned long DISPENSE_TURN_MS = 250;
static const unsigned long DISPENSE_DWELL_MS = 150; // let the coin drop

void initDispenser()
{
  pinMode(MOTOR_IN1, OUTPUT);
  pinMode(MOTOR_IN2, OUTPUT);
  digitalWrite(MOTOR_IN1, LOW);
  digitalWrite(MOTOR_IN2, LOW);
}

static void motorForward()
{
  digitalWrite(MOTOR_IN1, HIGH);
  digitalWrite(MOTOR_IN2, LOW);
}

static void motorReverse()
{
  digitalWrite(MOTOR_IN1, LOW);
  digitalWrite(MOTOR_IN2, HIGH);
}

static void motorStop()
{
  digitalWrite(MOTOR_IN1, LOW);
  digitalWrite(MOTOR_IN2, LOW);
}

// Release exactly one coin: turn 90 deg, wait for the coin to fall, turn back.
void dispenseOneCoin()
{
  motorForward();
  delay(DISPENSE_TURN_MS);
  motorStop();
  delay(DISPENSE_DWELL_MS);

  motorReverse();
  delay(DISPENSE_TURN_MS);
  motorStop();
}

// Pay out n coins one at a time, decrementing the shared count as we go so the
// app sees the jackpot drain to zero.
void dispenseCoins(uint8_t n)
{
  Serial.printf("Dispensing %u coin(s)\n", n);
  while (n > 0 && coinCount > 0)
  {
    dispenseOneCoin();
    coinCount--;
    notifyCoinCount();
    n--;
    delay(100);
  }
}
