#include "sampleData.h"
#include <SoftwareSerial.h>

SoftwareSerial Bluetooth(2,3);

#define  AD8232       A0
#define  pulse        A1
#define  ledPin       5

unsigned  long currentMicros  = 0;
unsigned  long previousMicros = 0;

const     long PERIOD         = 5000;

int outECG[2];
int outPPG[2];
char out[100];

int i = 0;
int counterLine = 0;
String out1,out2;
void setup() {
  Bluetooth.begin(115200);
  Serial.begin(9600);
  pinMode(5,OUTPUT);
  pinMode(6,OUTPUT);
  digitalWrite(5, 0);
  digitalWrite(6, 10);
  delay(1000);
  digitalWrite(6,0);
}

void loop() {
  currentMicros = micros ();
  if (currentMicros - previousMicros >= PERIOD){
    previousMicros = currentMicros;
    int next_ecg_pt = analogRead(AD8232);
    int next_ppg_pt = analogRead(pulse);
    //next_ppg_pt = map(next_ppg_pt,0,1023,0,512);
    Serial.println(next_ppg_pt);
    
    if(counterLine == 2){
      counterLine = 0;
      sprintf(out,"%03d*%03d*%03d*%03d",outECG[0],outECG[1], outPPG[0],outPPG[1]);
      Bluetooth.write(out);
    }else{
      outPPG[counterLine] = next_ppg_pt;
      outECG[counterLine] = next_ecg_pt;
      counterLine++;
    }
    //i++;
    //if(i>200) i = 0;
  }
}
