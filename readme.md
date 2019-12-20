# Pullprinting
Pull printing is the set of technologies and processes that allow print jobs to be released according to specific conditions; typically user authentication and proximity to a printer. This project is an open-source pull printing infrastructure based on Google CloudPrint that leverages the OpenID Connect protocol and the electronic IDentity (eID) card to protect users prints with a second-factor authentication. 

The goal of the project is to prevent print-related data breaches and tackle the challenges that hinder the widespread adoption of more secure printing solutions: costs and user experience. Additional information can be found at https://sites.google.com/fbk.eu/pullprinting.

## Requirments
- Android mobile devices with NFC;
- eID cards, such as the Italian CIE 3.0;
- An OAuth2 server with the support of the Authorization Code Grant flow and (server-side) the second-factor authentication, such as the [Authentication and Authorization Control Module](https://github.com/scc-digitalhub/AAC) (AAC) developed by Smart Community;
- An android mobile eID authenticator to support (client-side) the second-factor authentication, such as the one developed by Smart Community;
- A server to host the Print server and the Virtual Printer.

## Installation

### Google authentication configuration
- Open the [Google console](https://console.developers.google.com) with the account that will be responsible for the pull printing service (hereafter IT admin account);
- Create a new project;
- Choose the creation of a **ID client OAuth 2.0** for the mobile application in the credential section;
 - Select **Android** application as type and note the received **ClientID**;
- Create another **ID client OAuth 2.0** credential for the server application;
 - If using the Smart community AAC, to support their ID authenticator select **Web application** and note the **ClientID** and a **ClientSecret**;
  - Then, enable the following redirect URI in the console:
  ```
 https://tn.smartcommunitylab.it/timbrature/oauth
 https://am-test.smartcommunitylab.it/aac
 https://am-test.smartcommunitylab.it/aac/auth/google-oauth/callback
 https://appauth.demo-app.io/oauth2redirect
 ```
 - Otherwise, select **Android** application and configure the redirect URIs to the OAuth2 server implementation.
- Create an API key;
- Open Oauth from the menu and enable the following **scopes**:
```
email
profile
openid
https://www.googleapis.com/auth/cloudprint
```
- Then, enable the following domains:
```
smartcommunitylab.it
demo-app.io
```

### CUPS and Print Server installation
- Install CUPS. In Debian-based systems, use: 
```shell
sudo apt install cups
```
- Open the web interface at http://127.0.0.1:631;
- From the '*Administration*' panel, click on '*Add Printer*' and follow the steps suggested to create a generic printer with a generic driver (that will be used as our **Print Server**).

### GCP connector
- Install the GCP conncector to connect the CUPS spooler with Google Cloud Print
```shell
apt install google-cloud-print-connector
```
- Then init the configuration
```shell
gcp-connector-util init
```
- an example:  
```shell
Enable local printing? y
Enable cloud printing? y
Retain the user OAuth? y
User or group email to share with? <your-address@your-gsuite-domain>
Proxy name? <whatever>
  ```
- Run the GCP connector 
```shell
gcp-cups-connector -config-filename gcp-cups-connector.config.json
```
- (Optional) Run the connnector in background on the server.
```shell
gcp-cups-connector -config-filename gcp-cups-connector.config.json &
```

### CloudPrint install verification
- Access with the IT admin account to *Google Cloud Print* at the link https://www.google.com/cloudprint and verify a new printer called **Print Server** is present.
- Click on the *Print Server* and note the **Printer ID** from *Advanced Details* in the detail section.

### Backend installation
- Create a new Linux virtual machine with a static **IP address ** network configuration.
- (Optional) Assign a new entry in your DNS to give an **FQDN** to your virtual machine.
- Download the folder **PullprintingServer** from GitHub.
- Extract the admin refresh token with Google and insert this into **server.js** 
```shell
var adminRefreshToken = "1/Ev7Jv5w3xOPdy67n7NIwMvVo7im-kx2CuBnj1P0Q2JnyT1kF4tquS4-itHrQWaeF";
```
- Start the server with:
```shell
node PullprintingAPI/server.js
```

### Mobile application installation
- Download the **PullprintingMobileApp** from GitHub.
- Insert the **Printer ID** obtained during the Cloudprint install verification as the value *spoolerID* in the configuration file **Commons.java**;
- Insert the Print Server IP address or FQDN (if set during the backend installation) in **Commons.java** (variable *ipaddress*). For instance:
```java
public static String ipaddress = "pullprinting.fbk.eu";
public static String spoolerID = "a8d6c750-f342-f616-5a67-9188eacfdee2";
```
- Complete the file **auth_config.js** using the **clientID** found in your Google console using it as follow:
```java
"client_id": "CLIENT_ID_PREFIX.apps.googleusercontent.com",
"client_secret":"",
"redirect_uri":"com.googleusercontent.apps.CLIENT_ID_PREFIX:/oauth2redirect",
```
- If using Smart Community AAC, configure the **clientID** and **ClientSecret** obtained during the Google authentication configuration in **auth_config_sc.js** as follow:
```java
"client_id": "CLIENT_ID_PREFIX.apps.googleusercontent.com",
"client_secret":"383ef7c1-7718-4265-8108-f098b769c5e9", //secret mobile
"redirect_uri":"com.googleusercontent.apps.CLIENT_ID_PREFIX:/oauth2redirect",
```
- Otherwise configure the OAuth2 server according to the specific implementation.
