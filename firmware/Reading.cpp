#include <Arduino.h>
#include <FS.h>

struct Reading
{
  float acceleration;
  float altitude;
  uint32_t timestamp;

  void load(File& file) const;
  void save(File& file) const;
};

void Reading::save(File& file) const 
{
  file.write((uint8_t *)&acceleration, sizeof(acceleration));
  file.write((uint8_t *)&altitude, sizeof(altitude));
  file.write((uint8_t *)&timestamp, sizeof(timestamp));
}

void Reading::load(File& file) const
{
  file.read((uint8_t *)&acceleration, sizeof(acceleration));
  file.read((uint8_t *)&altitude, sizeof(altitude));
  file.read((uint8_t *)&timestamp, sizeof(timestamp));
}

struct Flight {
  std::vector<Reading> readings;
  void load(File& file);
  void save(File& file) const;
};

void Flight::save(File& file) const 
{
  auto n_readings = readings.size();
  file.write((uint8_t *)&n_readings, sizeof(n_readings));
  for (const auto& reading: readings)
    file.write((uint8_t*)&reading, sizeof(reading));
}

void Flight::load(File& file) 
{
  std::size_t n_readings;
  file.read((uint8_t *)&n_readings, sizeof(n_readings));

  readings.clear();
  readings.resize(n_readings);
  for (auto& reading: readings) {    
    file.read((uint8_t*)&reading, sizeof(reading));
  }
}

/*
class Reading {
  public:
    Reading() {};
    Reading(float acc, float alt, int time) {
      acceleration = acc;
      alt = alt;
      timestamp = time;
    }
    float acceleration;
    float altitude;
    int timestamp;
};*/