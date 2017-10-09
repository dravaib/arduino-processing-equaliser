import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 
import ddf.minim.analysis.*; 
import processing.serial.*; 
import javax.swing.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class EQarduino_processing extends PApplet {





Serial myPort;
Minim minim;
AudioInput in;
FFT fft;
PeakThread peakThread;



PFont font;
boolean has_serial = false;
float[] peaks;
int[] abars;
int customGain;
int bp = 250;
int gp = 0;
int rp = 0;
int iBrightness = 30;
String[] abarsString;

HScrollbar redscroll;
HScrollbar greenscroll;
HScrollbar bluescroll;
HScrollbar brightness;


final int[] freqs = {4, 6, 12, 25, 40, 80, 250, 400};
final int GAIN  = 3;
final int BAR_WIDTH = 8;
final int BAR_HEIGHT = 5;
final int DELAY = 20;
final int SAMPLING_FREQUENCY = 44100;
final int SAMPLING_DATA_RATE = 4096;
final int SERIAL_BAUD_RATE = 250000;
final int FRAME_RATE = 360;
final int WINDOW_WIDTH = 1024;
final int WINDOW_HEIGHT = 768;
final int RECT_WIDTH = 50;
final int RECT_HEIGHT = 50;
final int PEAK_HEIGHT_RATIO = 1;
final int PEAK_FALL_STEP = RECT_HEIGHT;
final boolean WAVE_ON = false;
final boolean DEBUG = false;


public void setup()
{
  if (DEBUG) {
    printArray(Serial.list());
  }

  if (Serial.list().length == 0) {
    println("No COM port found");
  } else {
    myPort = new Serial(this, Serial.list()[0], SERIAL_BAUD_RATE);
    has_serial = true;
  }

  font = createFont("Arial", 16, true); 
  textFont(font);       
  fill(255);

  minim = new Minim(this);
  in = minim.getLineIn(Minim.MONO, SAMPLING_DATA_RATE, SAMPLING_FREQUENCY);
  fft = new FFT(in.left.size(), SAMPLING_FREQUENCY);
  peaks = new float[BAR_WIDTH];
  abars = new int[BAR_WIDTH];
  abarsString = new String[BAR_WIDTH];

  frameRate(FRAME_RATE);
  

  bluescroll = new HScrollbar(width/2-255, 100, 255, 16, 16);
  greenscroll = new HScrollbar(width/2-255, 150, 255, 16, 16);
  redscroll = new HScrollbar(width/2-255, 200, 255, 16, 16);
  brightness = new HScrollbar(width/2-255, 250, 255, 16, 16);


  bluescroll.setPos(bp+275);

  customGain = 0;

  for (int i=0; i<peaks.length; i++) {
    peaks[i] = 0;
  }

  peakThread = new PeakThread();
  peakThread.start();
}

public void draw()
{
  background(0);
  stroke(255);
  strokeWeight(1);


  bluescroll.update();
  bluescroll.display(0, 0, 255);
  greenscroll.update();
  greenscroll.display(0, 255, 0);
  redscroll.update();
  redscroll.display(255, 0, 0);


  text("Brightness", width/2-215, 235);
  brightness.update();
  brightness.display(20, 20, 20);


  if (brightness.locked) {
    iBrightness = (int) brightness.getPos()-275;
    iBrightness = iBrightness > 200 ? 200 : iBrightness;
    if (has_serial) {
      myPort.write("l" + iBrightness + "\n");
    }
  }

  if (bluescroll.locked) {
    bp = (int) bluescroll.getPos() - 275;
    if (has_serial) {
      myPort.write("b" + bp + "\n");
    }
  }

  if (greenscroll.locked) {
    gp = (int) greenscroll.getPos() - 275;
    if (has_serial) {
      myPort.write("g" + gp + "\n");
    }
  }

  if (redscroll.locked) {
    rp = (int) redscroll.getPos() - 275;
    if (has_serial) {
      myPort.write("r" + rp + "\n");
    }
  }

  fill(rp, gp, bp);
  rect(width/2+100, 100, 100, 100);


  fft.forward(in.left);

  int peakHeight = RECT_HEIGHT / PEAK_HEIGHT_RATIO;
  int barNumber = 0;

  for (int n : freqs) {

    float band = fft.getBand(n) * (GAIN + customGain);      
    int barOffsetX = barNumber * RECT_WIDTH;
    int currentBars = ((int) band) / RECT_HEIGHT;
    currentBars = currentBars > BAR_HEIGHT ? BAR_HEIGHT : currentBars;

    if (abars[barNumber] < currentBars) {
      currentBars = abars[barNumber]--;
      abars[barNumber] = currentBars+1;
    } else if (abars[barNumber] > currentBars) {
      currentBars = abars[barNumber]++;
      abars[barNumber] = currentBars-1;
    }

    abarsString[barNumber] = str(abars[barNumber]);

    for (int i=0; i<(currentBars); i++) {
      fill(rp, gp, bp);
      rect(barOffsetX, height - (i * RECT_HEIGHT), RECT_WIDTH, RECT_HEIGHT);
    }

    if (peaks[barNumber] < band) {
      peaks[barNumber] = currentBars * RECT_HEIGHT;
    }

    fill(255, 0, 0);
    rect(barOffsetX, height - peaks[barNumber], RECT_WIDTH, peakHeight);

    fill(255);
    textAlign(CENTER);
    text("Gain: " + customGain, 45, 30); 
    if (has_serial) {
      text("Arduino: " + Serial.list()[0], 50, 50);
    } else {
      text("Arduino: Not connected", 100, 50);
    }


    barNumber++;
  }

  if (WAVE_ON) {
    wave();
  }

  String peakString = "";

  for (int p=peaks.length-1; p>=0; p--) {
    peakString += (int)peaks[p]/RECT_HEIGHT;
  }

  String send = "i" + join(reverse(abarsString), "") + peakString  + "\n";

  if (DEBUG) println(send + "|" + abarsString.length);

  if (has_serial) {
    myPort.write(send);

    if (DEBUG) {
      println(myPort.read());
    }
  }

  delay(DELAY);
}


public class PeakThread extends Thread
{
  public void start()
  {
    super.start();
  }
  public void run()
  {
    while (true) {
      float max = 0;

      for (int i=0; i<BAR_WIDTH; i++) {
        if (abars[i] < peaks[i]) {
          peaks[i] -= PEAK_FALL_STEP;
          max = max < peaks[i] ? peaks[i] : max;
        }
      }

      max /= RECT_HEIGHT;
      max = 150 * max;
      max = max < DELAY ? DELAY*2 : max;
      delay((int) max);
    }
  }
}

class HScrollbar {
  int swidth, sheight;    // width and height of bar
  float xpos, ypos;       // x and y position of bar
  float spos, newspos;    // x position of slider
  float sposMin, sposMax; // max and min values of slider
  int loose;              // how loose/heavy
  boolean over;           // is the mouse over the slider?
  boolean locked;
  float ratio;

  HScrollbar (float xp, float yp, int sw, int sh, int l) {
    swidth = sw;
    sheight = sh;
    int widthtoheight = sw - sh;
    ratio = (float)sw / (float)widthtoheight;
    xpos = xp;
    ypos = yp-sheight/2;
    spos = xpos + swidth/2 - sheight/2;
    newspos = spos;
    sposMin = xpos;
    sposMax = xpos + swidth - sheight;
    loose = l;
  }

  public void update() {
    if (overEvent()) {
      over = true;
    } else {
      over = false;
    }
    if (mousePressed && over) {
      locked = true;
    }
    if (!mousePressed) {
      locked = false;
    }
    if (locked) {
      newspos = constrain(mouseX-sheight/2, sposMin, sposMax);
    }
    if (abs(newspos - spos) > 1) {
      spos = spos + (newspos-spos)/loose;
    }
  }

  public float constrain(float val, float minv, float maxv) {
    return min(max(val, minv), maxv);
  }

  public boolean overEvent() {
    if (mouseX > xpos && mouseX < xpos+swidth &&
      mouseY > ypos && mouseY < ypos+sheight) {
      return true;
    } else {
      return false;
    }
  }

  public void display(int red, int green, int blue) {
    fill(100);
    rect(xpos, ypos, swidth, sheight);
    /*if (over || locked) {
     //fill(0);
     } else {*/
    fill(red, green, blue);
    //}
    rect(spos, ypos, sheight, sheight);
  }

  public void setPos(float p) {
    newspos = constrain(p, sposMin, sposMax);
  }

  public float getPos() {
    // Convert spos to be values between
    // 0 and the total width of the scrollbar
    return spos * ratio;
  }
}

public void keyPressed() {
  switch(keyCode) {
  case 139:
    customGain++;
    break;
  case 140:
    customGain--;
    break;
  case 68:
    if (has_serial) myPort.write("d\n");
    break;
  case 80:
    if (has_serial) myPort.write("p\n");
    break;
  case 86:
    if (has_serial) myPort.write("v\n");
    break;
  case 66:
    if (has_serial) myPort.write("b\n");
    break;
  case 67:
    if (has_serial) myPort.write("c\n");
    break;
  case 77: 
    {
      this.frame.setExtendedState(JFrame.ICONIFIED);
        println("minimize...");

      break;
    }
  case 88: 
    {
      if (has_serial) myPort.write("x\n");
      break;
    }
  }
  println("pressed: " + keyCode);

  println("gain: " + customGain);
}

public void wave() {
  // draw the waveforms
  int a = 100;
  int b = a*2;
  strokeWeight(3);
  for (int i = 0; i < in.bufferSize() - 1; i++)
  {
    line(i, a + in.left.get(i)*b, i+1, a + in.left.get(i+1)*b);
  }
}

public void stop()
{
  in.close();
  minim.stop();
  super.stop();
}

  public void settings() {  size(1024, 600, P2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "EQarduino_processing" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
