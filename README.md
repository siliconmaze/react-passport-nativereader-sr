
# react-native-passport-reader-sr

Adapted from [passport-reader](https://github.com/tradle/react-native-passport-reader). Essentially upgraded to use JMRTD 7.0.18 as oposed to 5.5.0

## Getting started

```sh
$ npm install react-native-passport-reader-sr --save
$ react-native link react-native-passport-reader-sr
```

In your `android/app/build.gradle` add `packagingOptions`:

```
android {
    ...
    packagingOptions {
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/NOTICE'
    }
}
```

In `AndroidManifest.xml` add:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

If your app will not function without nfc capabilities, set `android:required` above to `true`

## Usage
```js
import PassportReader from 'react-native-passport-reader-sr'
// { scan, cancel, isSupported }

async function scan () {
  // 1. start a scan
  // 2. press the back of your android phone against the passport
  // 3. wait for the scan(...) Promise to get resolved/rejected

  const { 
    firstName, 
    lastName, 
    gender, 
    issuer, 
    nationality, 
    photo 
  } = await PassportReader.scan({
    // yes, you need to know a bunch of data up front
    // this is data you can get from reading the MRZ zone of the passport
    documentNumber: 'ofDocumentBeingScanned',
    dateOfBirth: 'yyMMdd',
    dateOfExpiry: 'yyMMdd'
  })

  const { base64, width, height } = photo
}
```
