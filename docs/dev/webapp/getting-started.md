# Getting Started

The Web App is a single page application written in TypeScript with Angular 5.
If you are new to Angular you can check out the [Angular Tour of Heroes](https://angular.io/tutorial) it will help you getting started.

## Development environment setup

### Editor 

There are two editors that can be recommended:

- [JetBrains WebStorm](https://www.jetbrains.com/webstorm/) (non-free) 
- [Visual Studio Code](https://code.visualstudio.com/) (open source)

Both of them have excellent TypeScript and Angular 5 support.

### Package manager

This project uses the [yarn](https://yarnpkg.com) package manager. It is also possible to use [npm](https://www.npmjs.com/). 

### Angular CLI

The  [Angular CLI](https://cli.angular.io/) is used to serve and build our web app. 
It also can be used to create components, services and much more.
Read its [documentation](https://github.com/angular/angular-cli/wiki) to check out how it is installed and what else it is feasible of.

### CORS Plugin

Since the idea of the WebApp is to be deployed with the API on the same server you need a plugin that allows cross origin requests.

- Firefox: [CORS Everywhere](https://addons.mozilla.org/en-US/firefox/addon/cors-everywhere/?src=search)   
- Chrome/Chromium: [Allow-Control-Allow-Origin: \*](https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi)

## Get the WebApp running

### SetUp the repository
The WebApp is part of our TOSCAna repository therefore you need to clone the whole repository to develop the WebApp.
```bash
git clone git@github.com:StuPro-TOSCAna/TOSCAna.git
```

Now you have to install all packages the WebApp needs to be build.

```bash
cd TOSCAna && yarn install
```

### Start the TOSCAna transformator

If you want to use the WebApp in production you should start it with the TOSCAna transformator like descriped [here](../../user/getting-started.md).
For development this is quite heavy and features like live reload are missing. Therefore you should run the transformator independently. 

### Set the environment
If the TOSCAna transformator is running, you have to tell the WebApp its REST API address.

Therefore edit `app/src/environment/environment.ts`.
```js
export const environment = {
    production: false,
    apiUrl: 'http://localhost:8084'
};
```
Change the `apiUrl` to the URL of the transformator REST API:

If you change the `environment.prod.ts` the deployment with the rest of the stack wont work.

### Serve the WebApp

In the app root folder run:
```bash
yarn start
```
You now can view the WebApp in your browser at this address: [http://localhost:4200](http://localhost:4200).