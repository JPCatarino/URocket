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

struct Flight {
  std::vector<Reading> readings;
  void load(File& file);
  void save(File& file) const;
};