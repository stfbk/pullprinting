# Pullprinting
Pull printing is the set of technologies and processes that allow print jobs to be released according to specific conditions; typically user authentication and proximity to a printer. We developed an open-source pull printing infrastructure based on Google CloudPrint that leverages the OpenID Connect protocol and the electronic IDentity (eID) card to protect users prints with a second-factor authentication. Our goal is to prevent print-related data breaches and tackle the challenges that hinder the widespread adoption of more secure printing solutions: costs and user experience. Additional information can be found at https://sites.google.com/fbk.eu/pullprinting.

##Requirment
- NFC reader in your phone to read the physical CIE
- Authentication and Authorization Control Module (AAC) to enable and response to the CIE reading (here you can find an example https://github.com/scc-digitalhub/AAC)
- UNIX virtual machine to set up the server with the API for the application

##Installation

###Google console
- Open Google console with your Google administrator account
`<link>` : <https://console.developers.google.com>
- Create a new project for the pullprinting
- Create under credential an **ID client OAuth 2.0 ** for the mobile application
 - select **Android** application
 - this will give you a **ClientID**
- Create another **ID client OAuth 2.0 ** credential for the server application
 - select **Web application**
 - this will give you a **ClientID** and a **ClientSecret**
 - enable the following redirect URI in the console:
 ```
https://tn.smartcommunitylab.it/timbrature/oauth
https://am-test.smartcommunitylab.it/aac
https://am-test.smartcommunitylab.it/aac/auth/google-oauth/callback
https://appauth.demo-app.io/oauth2redirect
```
- Create an API key
- Open Oauth from the menu
- Enable the following **scope**:
```
email
profile
openid
https://www.googleapis.com/auth/cloudprint
```
- Enable the following domain:
```
smartcommunitylab.it
demo-app.io
```

###CUPS and virtual spooler installation
- Install CUPS
```shell
sudo apt install cups
```
- Open web application at port 631
`<link>` : <http://127.0.0.1:631>
- Go inside the '*Administration*' panel and click on '*Add Printer*'
- Follow the steps and create a generic printer with a generic driver that will be used as **virtual spooler**

###CloudPrint installation
- Access with the chosen administrator account into *Google Cloud Print* at the link
`<link>` : <https://www.google.com/cloudprint>.
- In your predefined Google account, you will see now a new printer called **virtual spooler**.
- Click on the *virtual spooler* printer and then select *detail* button you can found in the *Advanced Details* the **Printer ID**.

###Backend installation
- Create a new Unix virtual machine.
- Give to the machine a static **IP address **.
- (Optional) Assign a new A entry in your DNS to give an **FQDN** at your virtual machine.
- Download the folder **PullprintingServer** from GitHub.
- Put the content of the fold in your root virtual machine.
- extract the admin refresh token with Google and insert this into **server.js** 
```shell
var adminRefreshToken = "1/Ev7Jv5w3xOPdy67n7NIwMvVo7im-kx2CuBnj1P0Q2JnyT1kF4tquS4-itHrQWaeF";
```
- It is a node application, for this reason, you have to go inside the file "*PullprintingAPI*" (CHECK-CHANGE) and to start the server use the 
command:
```shell
node server.js
```
or
```shell
node server.js &
```

###Mobile application installation
- Now you have to downaload the **PullprintingMobileApp** from GitHub.
- Change in the configuration file **Commons.java** the variable *spoolerID* in the mobile application putting the **Printer ID** found on Google Cloud Print.
- Change in the configuration file **Commons.java** the variable *ipaddress* in the mobile application putting the **IP address** or **FQDN** of your server.
```java
public static String ipaddress = "pullprinting.fbk.eu";
public static String spoolerID = "a8d6c750-f342-f616-5a67-9188eacfdee2";
```
- Complete the file **auth_config.js** using the **clientID** found in your Google console use it as follow:
```java
"client_id": "CLIENT_ID_PREFIX.apps.googleusercontent.com",
"client_secret":"",
"redirect_uri":"com.googleusercontent.apps.CLIENT_ID_PREFIX:/oauth2redirect",
```
- Exchange your **clientID** and **ClientSecret** found in your Google console with your AAC
- Complete the file **auth_config_sc.js** using the **clientID** and **ClientSecret** exchanged:
```java
"client_id": "CLIENT_ID_PREFIX.apps.googleusercontent.com",
"client_secret":"383ef7c1-7718-4265-8108-f098b769c5e9", //secret mobile
"redirect_uri":"com.googleusercontent.apps.CLIENT_ID_PREFIX:/oauth2redirect",
```