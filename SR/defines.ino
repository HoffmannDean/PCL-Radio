// I2S Pins
#define DIN_PIN 26   // serial data
#define LRCK_PIN 25  // word select
#define BCLK_PIN 27  // serial clock

// SPI Pins
#define PIN_SPI_MOSI   21
#define PIN_SPI_MISO   19
#define PIN_SPI_SCK    18

// SPI Chip Selects
#define PIN_SD_CS      17
#define PIN_LM1971_CS  4
// 74HC595
#define PIN_595_RCLK   23


// Rotary Encoder Direction Pins
#define ENCODER_A 13
#define ENCODER_B 33

//Rotary Encoder Push Button Pin
#define ENCODER_BTN 35

//Analog-Pin Battery Voltage
#define BATTERY_SENSE 34

// --- Coin acceptor (merged from the standalone coin-counter sketch) ---
// Analog sensor that detects an inserted coin. The old sketch used GPIO 34,
// but that pin is BATTERY_SENSE here, so the coin sensor moves to GPIO 32
// (also ADC1, so it keeps working while WiFi/BT are active).
#define COIN_SENSE 32

// Always-on output the old sketch drove HIGH at boot (its "sourcePin", used to
// power the sensor / light an indicator LED). It was GPIO 4 originally, but
// that pin is PIN_LM1971_CS here, so it moves to GPIO 15. Confirm vs wiring.
#define COIN_SENSOR_PWR 15

// Indicator LED that pulses each time a coin is detected (the old "alertPin",
// GPIO 2 -- the on-board LED on most ESP32 dev boards).
#define COIN_LED 2

// --- Coin dispenser motor (H-bridge) ---
// Two direction inputs of the H-bridge (e.g. L298N IN1/IN2). A win turns the
// motor 90 deg to release one coin, then back. These are placeholder GPIOs;
// confirm against the actual wiring before running the motor for real.
#define MOTOR_IN1 14
#define MOTOR_IN2 16

