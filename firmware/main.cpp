#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_BMP280.h>
#include <MPU6050_tockn.h>
#include <CircularBuffer.h>
#include "FS.h"
#include "SPIFFS.h"
#include "BluetoothSerial.h"
#include "Reading.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

#define READING_INTERVAL 50
#define MOVEMENT_THRESHOLD 0.2
#define COUNTER_THRESHOLD 30
#define BUFFER_SIZE 20

auto constexpr DATA_FILE = "/data.bin";

bool isMoving = false;
float baseAcc;
float baseAlt;
uint8_t counter = 0;
uint32_t timerAdjustment = 0;

byte address [126];
byte nDevices = 0;

Adafruit_BMP280 bmp;
MPU6050 mpu(Wire);

BluetoothSerial SerialBT;

CircularBuffer<Reading, BUFFER_SIZE> buffer;
Flight flight;
bool isReadyToTransmit;

long timer = 0;
String uid = "";

float getTotalAcceleration() {
  mpu.update();
  return sqrt(pow(mpu.getAccX(),2)+pow(mpu.getAccY(),2)+pow(mpu.getAccZ(),2));
}

void getAllData() {
  // ======
  // BMP280
  // ======
  Serial.print("Altitude: ");
  Serial.print(bmp.readAltitude());
  Serial.print("\n");
  // =======
  // MPU6050
  // =======
  mpu.update();
  //Serial.println(mpu.getTemp());
  Serial.print("Acceleration: (");
  Serial.print(mpu.getAccX());
  Serial.print(", ");
  Serial.print(mpu.getAccY());
  Serial.print(", ");
  Serial.print(mpu.getAccZ());
  Serial.print(")\n");

  Serial.print("Gyro: (");
  Serial.print(mpu.getGyroX());
  Serial.print(", ");
  Serial.print(mpu.getGyroY());
  Serial.print(", ");
  Serial.print(mpu.getGyroZ());
  Serial.print(")\n");

  Serial.print("Acceleration Angle: (");
  Serial.print(mpu.getAccAngleX());
  Serial.print(", ");
  Serial.print(mpu.getAccAngleY());
  Serial.print(")\n");

  Serial.print("Gyro Angle: (");
  Serial.print(mpu.getGyroAngleX());
  Serial.print(", ");
  Serial.print(mpu.getGyroAngleY());
  Serial.print(", ");
  Serial.print(mpu.getGyroAngleZ());
  Serial.print(")\n");
  
  Serial.print("Angle: (");
  Serial.print(mpu.getAngleX());
  Serial.print(", ");
  Serial.print(mpu.getAngleY());
  Serial.print(", ");
  Serial.print(mpu.getAngleZ());
  Serial.print(")\n\n\n\n");
}

void sendData() {
  if(!isReadyToTransmit)
    SerialBT.printf("NULL\n\r");
  else {

    if(!SPIFFS.begin()){
      Serial.println("An Error has occurred while mounting SPIFFS");
      return;
    }

    File file = SPIFFS.open(DATA_FILE, "r");
    flight.load(file);
    file.close();
    SPIFFS.end();
    for(Reading r : flight.readings) {
      String str = F("{acceleration:");
      str += String(r.acceleration,2);
      str += F(",altitude:");
      str += String(r.altitude,2);
      str += F(",timestamp:");
      str += String(r.timestamp);
      str += F("}\n\r");
      Serial.println(str);
      SerialBT.printf(str.c_str());
      delay(10);
    }
    flight.readings.clear();
    delay(1000);
    SerialBT.printf("OVER\n\r");
  }
}

void sendError() {
  SerialBT.printf("ERROR\n\r");
}

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_Rocket");
 
  if(!bmp.begin()) {
    Serial.print("Unable to initialize BMP.\n");
  }
  baseAlt = bmp.readAltitude();
  Wire.begin();
  mpu.begin();
  mpu.calcGyroOffsets(true);
  Serial.print("\n");
  baseAcc = getTotalAcceleration();
  SPIFFS.begin();
  isReadyToTransmit = SPIFFS.exists(DATA_FILE);
  SPIFFS.end();
}

void loop() {
  if(millis() - timer >= READING_INTERVAL) {
    timer = (uint32_t)millis();
    float acc = getTotalAcceleration();
    //Serial.println(acc);
    if((acc>baseAcc+MOVEMENT_THRESHOLD || acc<baseAcc-MOVEMENT_THRESHOLD) && !isMoving) {
      Serial.print("Rocket is moving\n");
      isMoving = true;
      int i;
      Reading tempReads [BUFFER_SIZE];
      for(i=0; i<BUFFER_SIZE; i++) {
        tempReads[i] = buffer.pop();
        if(i==0)
          timerAdjustment = tempReads[0].timestamp;
        else if(tempReads[i].timestamp<timerAdjustment)
          timerAdjustment = tempReads[i].timestamp;
      }
      for(i=0; i<BUFFER_SIZE; i++) {
        tempReads[i].timestamp -= timerAdjustment;
        tempReads[i].altitude -= baseAlt;
        flight.readings.push_back(tempReads[i]);
      }
      buffer.clear();
    }
    if(isMoving) {
      if(acc<baseAcc+MOVEMENT_THRESHOLD && acc>baseAcc-MOVEMENT_THRESHOLD) {
        counter++;
        if(counter>COUNTER_THRESHOLD) {
          Serial.print("Rocket stopped\n");
          isMoving = false;
          counter = 0;
          isReadyToTransmit = true;
          if(!SPIFFS.begin()){
            Serial.println("An Error has occurred while mounting SPIFFS");
            return;
          }
          File file = SPIFFS.open(DATA_FILE, "w");
          flight.save(file);
          flight.readings.clear();
          file.close();
          SPIFFS.end();
        }
      }
      flight.readings.push_back({acc,bmp.readAltitude()-baseAlt,timer-timerAdjustment});
    }
    else
      buffer.push({acc,bmp.readAltitude(),timer});
  }
  if (SerialBT.available()) {
    String receivedMessage = SerialBT.readString();
    Serial.print("BT Message received: ");
    Serial.print(receivedMessage);
    Serial.print("\n");
    if(receivedMessage.indexOf('[')==0 && receivedMessage.indexOf(']')>-1) {
      if(uid=="") {
        uid = receivedMessage.substring(1,receivedMessage.indexOf(']'));
        Serial.print("Connected to ");
        Serial.print(uid);
        Serial.print("\n");
        SerialBT.printf("Connected\n\r");
      }
      else if (uid==receivedMessage.substring(1,receivedMessage.indexOf(']'))) {
        sendData();
      }
      else {
        sendError();
      }
    }
  }
}