#include "FastLED.h"

#define NUM_LEDS 40
#define DATA_PIN 6
#define MAX_BAR_WIDTH 8
#define MAX_BAR_HEIGHT 5
#define BRIGHTNESS 20

String inputString = "";
boolean stringComplete = false;
int strl = 0;
int vals[8];
int vl = 0;
int val = 0;
int peak = 0;
int dotMode = false;
int peakMode = true;

int blue = 255;
int green = 0;
int red = 0;

CRGB color = CRGB::Blue;
CRGB leds[NUM_LEDS];

void setup() {
  FastLED.addLeds<WS2812, DATA_PIN, GRB>(leds, NUM_LEDS);
  FastLED.setBrightness(  BRIGHTNESS );
  Serial.begin(250000);

  inputString.reserve(200);
}

void loop() {
  if (stringComplete) {

    switch (inputString[0]) {
      case 'i': {
          Serial.println("Input");

          for (int i = 1; i <=(strl - 1) / 2; i++) {

            val = (int) inputString[i] - 48;
            val = val > MAX_BAR_HEIGHT ? MAX_BAR_HEIGHT : val;
            val = val < 0 ? 0 : val;
            peak = (int) inputString[((strl - 1) / 2) + i] - 48;
            peak = peak > MAX_BAR_HEIGHT ? MAX_BAR_HEIGHT : peak;
            peak & peak < 0 ? 0 : peak;
            Serial.println(val);
            Serial.println("_");
            Serial.println(peak);
            
            int valreal = i-1 + MAX_BAR_WIDTH * (val - 1);
              for (int k = 1; k <= MAX_BAR_HEIGHT; k++) {
                valreal = i-1 + MAX_BAR_WIDTH * (k - 1);

                if(peakMode) {
                  if(k == peak) {
                    leds[valreal] = CRGB::Red;
                  }
                  else if(!dotMode && k <= val && k < peak) {
                    leds[valreal].blue = blue;
                    leds[valreal].red = red;
                    leds[valreal].green = green;
                  }
                  else if(dotMode && k==val && k < peak) {
                    leds[valreal].blue = blue;
                    leds[valreal].red = red;
                    leds[valreal].green = green;
                  }
                  else {
                    leds[valreal] = CRGB::Black; 
                  }
                } else {
                  if(!dotMode && k <= val) {
                    leds[valreal].blue = blue;
                    leds[valreal].red = red;
                    leds[valreal].green = green;
                  }
                  else if(dotMode && k==val) {
                    leds[valreal].blue = blue;
                    leds[valreal].red = red;
                    leds[valreal].green = green;
                  }
                  else {
                    leds[valreal] = CRGB::Black; 
                  }
                }
              }
          }
          
          FastLED.show();
          break;
        }

        case 'd': {
          dotMode = !dotMode;
          break;
        }

        case 'p': {
          peakMode = !peakMode;
          break;
        }

        case 'v': {
          color = CRGB::Green;
          break;
        }

        case 'c': {
         color = CRGB::Cyan;
         break;
        }
        case 'b': {
         blue = getColor();
         break;
        }
        case 'g': {
         green = getColor();
         break;
        }
        case 'r': {
         red = getColor();
         break;
        }

        case 'l': {
          FastLED.setBrightness(  getColor() );
          break;
        }

        case 'x': {
          for(int i = 0; i<NUM_LEDS; i++) {
            leds[i] = CRGB::Black;
            FastLED.show();
          }
        }
    }
    inputString = "";
    stringComplete = false;
    strl = 0;
  }
}


int getColor() {
  int m = strl > 4 ? 3 : strl;
  String clr = "";
  for(int i = 1; i <= m; i++) {
    clr += inputString[i];
  }

  return clr.toInt();
}

void serialEvent() {
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    if (inChar == '\n') {
      stringComplete = true;
    } else {
      inputString += inChar;
      strl++;
    }
  }
}


