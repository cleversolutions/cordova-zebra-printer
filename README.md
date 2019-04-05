# cordova-zebra-printer
A Cordova plugin for Zebra CPCL printers for both iOS and Android with Ionic 3 bindings. This plugin only supports Zebra models that use CPCL printing. Feel free to contribute to this project if you need to support other methods of printing. It has only been tested with Zebra QLn320 printers. Let me know if you use if sucessfully with others.

Get from npm
```
cordova plugin add ca-cleversolutions-zebraprinter
```
or get the latest version from git
```
cordova plugin add https://github.com/cleversolutions/cordova-zebra-printer.git
```

To use with Ionic 3

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