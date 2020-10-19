# Nuggets

Once I had placed my code, I ran `npm init` in this repo to create the `package.json`

## NPM Install

In this project create a new file called app.js. This is where you are going to use your new wrap-with-poo package. This is normally where you would normally install the npm package you needed by running the following command.

```s
npm install react-native-passport-reader-sr
```

The problem with this is that you haven't published your new plugin yet so it isn't in npm. You need a way to reference the package locally while you're developing it. You could run npm install with the absolute path to the package.

This is an example folder on my iMAc where the repo is located.
I change to my project folder where I wish to use this module and run:

```s
npm install /Users/stever/local-repos/github/react-passport-nativereader-sr
```

Example running in the root of a project where I wish to use the code.

```s
npm install /Users/stever/local-repos/github/react-passport-nativereader-sr
npm WARN @react-native-community/image-editor@2.3.0 requires a peer of react-native@>=0.57 <0.62 but none is installed. You must install peer dependencies yourself.
npm WARN @react-native-firebase/ml-vision@7.1.3 requires a peer of @react-native-firebase/app@7.2.0 but none is installed. You must install peer dependencies yourself.
npm WARN react-native-qrcode-svg@6.0.6 requires a peer of react-native-svg@^9.6.4 but none is installed. You must install peer dependencies yourself.
npm WARN redux-persist-transform-encrypt@2.0.1 requires a peer of redux-persist@^5.x.x but none is installed. You must install peer dependencies yourself.

+ react-native-passport-reader-sr@0.1.0
added 1 package from 1 contributor, removed 2 packages and audited 1404 packages in 7.392s

50 packages are looking for funding
  run `npm fund` for details

found 6 low severity vulnerabilities
  run `npm audit fix` to fix them, or `npm audit` for details
```

.. the issue I have now is that I really need to use a github URI so I can get it from the 'net, for now this is what package.json contains.

```s
"react-native-passport-reader": "^1.0.3",
    "react-native-passport-reader-sr": "file:../../../../github/react-passport-nativereader-sr",
```

We can see that is a local file and so this path will not resolve when I clone to my windows Machine. (I use two machines to test, one is Windows 2010, the other an iMAC)

## NPM Link
npm link is a process that allows you to create a symbolic link between your project and the dependency. First, you need to move into the directory wrap-with-poo and run the following command.

```s
npm link
```
This will take your package and create a symbolic link in the npm global folder to it.

```s
npm link
npm notice created a lockfile as package-lock.json. You should commit this file.
up to date in 0.328s
found 0 vulnerabilities


/Users/stever/.npm-global/lib/node_modules/react-native-passport-reader-sr -> /Users/stever/local-repos/github/react-passport-nativereader-sr
```

This means that your project can be used in any project with one more simple step. The next thing you need to do is to move into the project where we are testing and run the following command.

```s
npm link react-native-passport-reader-sr
```

This will output the following: 

```s
/Users/stever/local-repos/<-- REDACTED -->/node_modules/react-native-passport-reader-sr -> /Users/stever/.npm-global/lib/node_modules/react-native-passport-reader-sr -> /Users/stever/local-repos/github/react-passport-nativereader-sr
```

I can now update my packagejson to be:
`"react-native-passport-reader-sr": "file:/Users/stever/local-repos/github/react-passport-nativereader-sr",` instead of
`"react-native-passport-reader": "^1.0.3",`

Then i can test my making a change to my test project

Change my react-native code entries where `import PassportReader from 'react-native-passport-reader';` should now be `import PassportReader from 'react-native-passport-reader-sr';`

I then ran from within my project's repo

```s
npm install /Users/stever/local-repos/github/react-passport-nativereader-sr
```

now in the node_modules folder I have `react-native-passport-reader-sr` instead of `react-native-passport-reader`

in node_modules we see a symlink `react-native-passport-reader-sr -> ../../../../../github/react-passport-nativereader-sr`

But for me the application was always reporing that is cannot find the module, it is probably because the path is not a relative path

I basically then juse deleted and manually created the symlink

```s
cd /Users/stever/local-repos/codecommit/retail-age-verification/src/FujitsuVerify/node_modules
ln -s /Users/stever/local-repos/github/react-passport-nativereader-sr
```


We then see the following symlink has relative folder

```s
react-passport-native-reader-sr -> /Users/stever/local-repos/github/react-passport-nativereader-sr
```

This woul dnot work in react-native, I woul dget unable to resolve module, so I decided to reference the live github repo

## References
Links which I personally read to give some ideas
- https://docs.npmjs.com/creating-and-publishing-private-packages
- https://www.freecodecamp.org/news/how-to-make-a-beautiful-tiny-npm-package-and-publish-it-2881d4307f78/
- https://dev.to/therealdanvega/creating-your-first-npm-package-2ehf