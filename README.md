# cordova-zebra-printer
A Cordova plugin for Zebra printers for both iOS and Android with Ionic 3 bindings

To use with Ionic 3

I've not published this to cordova plugin registry yet so clone this repo somewhere then in your project
ionic cordova plugin add /path/to/this/plugin


Add the Ionic 3 bindings to your app.module.ts file
```
import { ZebraPrinter } from 'ca-cleversolutions-zebraprinter/native';
...
providers: [
    ZebraPrinter,
    ...
```

Then inject the ZebraPrinter into the constructor of the class where you wish to use it
for example
```
constructor(public navCtrl: NavController, protected zebraPrinter:ZebraPrinter) {
    console.log(">>>>>> Home Constructed <<<<<<<")
  }

  protected discover(){
    console.log("Now Discover");
    this.zebraPrinter.discover().then(result => {
      console.log(result);
    }).catch(err => {
      console.error(err);
    });
  }
```