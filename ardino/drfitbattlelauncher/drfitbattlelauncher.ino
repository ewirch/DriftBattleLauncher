

const byte CMD_PREFIX = 56;
const byte CMD_GET_TYPE = 7;
const byte CMD_LIGHT_ON = 11;
const byte CMD_LIGHT_OFF = 12;
const byte CMD_TONE_ON = 54;
const byte CMD_TONE_OFF = 55;
const byte CMD_TONE = 60;

const byte RESPONSE_OK = 2;
//const byte TYPE_RED_AND_GREEN = 42;
const byte TYPE_RED_GREEN_AND_ORANGE = 43;

//const byte LED_COUNT = 6;
const byte LED_COUNT = 9;
const byte RED_LED_COUNT = 4;
const byte ORANGE_LED_COUNT = 3;
// LED pins von "oben" nach "unten", angefangen bei den roten
byte LED_PIN_MAPPING[LED_COUNT] = {11,10,9,8, 7,6,13, 5,4};
const byte BUZZER_PIN = 3;

const int MAX_TONE_DURATION = 5000; // 5s

#define BT Serial

void setup() {
  //Serial.begin(115200);
  BT.begin(9600);
 
  for (int i = 0; i < LED_COUNT; i++) {
    byte led = LED_PIN_MAPPING[i];
    pinMode(led, OUTPUT);
    digitalWrite(led, LOW);
  }
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
}

void doTimerTasks();
void checkToneTimeout(long now);
void onCmdLightOn(int data);
void onCmdLightOff(int data);
void onCmdGetType();
void onCmdTone(int data);
void onCmdToneOn();
void onCmdToneOff();

void tone(uint32_t ulPin, uint32_t frequency, int32_t duration);
void noTone(uint32_t ulPin);

long toneStartTime = 0;
long toneDuration = 0;
long loops = 0;

void loop() {
  if (BT.available() >= 3) {
    int prefix = BT.read();
    if (prefix == CMD_PREFIX) {
      int cmd = BT.read();
      int data = BT.read();
      
      switch (cmd) {
        case CMD_GET_TYPE:
          onCmdGetType();
          break;
        case CMD_LIGHT_ON:
          onCmdLightOn(data);
          break;
        case CMD_LIGHT_OFF:
          onCmdLightOff(data);
          break;
        case CMD_TONE_ON:
          onCmdToneOn();
          break;
        case CMD_TONE_OFF:
          onCmdToneOff();
          break;
        case CMD_TONE:
          onCmdTone(data);
          break;
      };
    }
  }
  if (loops > 10000) {
    doTimerTasks();
    loops = 0;
  }
  loops++;
}

void doTimerTasks() {
  long now = millis();
  checkToneTimeout(now);
 }

void checkToneTimeout(long now) {
  if (toneStartTime != -1) {
    if ((now - toneStartTime) > toneDuration) {
      onCmdToneOff();
    }
  }
}

void onCmdLightOn(int data) {
  if (data >= 0 && data < LED_COUNT) {
    digitalWrite(LED_PIN_MAPPING[data], HIGH);
  }
}

void onCmdLightOff(int data) {
  if (data >= 0 && data < LED_COUNT) {
    digitalWrite(LED_PIN_MAPPING[data], LOW);
  }
}

void onCmdGetType() {
  BT.write(TYPE_RED_GREEN_AND_ORANGE);
  BT.write(RED_LED_COUNT | (ORANGE_LED_COUNT << 4));
  BT.write(LED_COUNT);
}

void onCmdTone(int data) {
  if (data > 0) {
    if (data > MAX_TONE_DURATION) data = MAX_TONE_DURATION;
    digitalWrite(BUZZER_PIN, HIGH);
    toneStartTime = millis();
    toneDuration = data;
  }
}
  
void onCmdToneOn() {
  onCmdTone(MAX_TONE_DURATION);
}

void onCmdToneOff() {
  digitalWrite(BUZZER_PIN, LOW);
  toneStartTime = -1;
}


