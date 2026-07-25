# Crowdsensing WoT — App iOS

Porting iOS nativo (Swift / SwiftUI) dell'app Android di crowdsensing basata
su Web of Things. Parla **lo stesso identico protocollo** dell'app Android,
quindi funziona con il backend esistente (proxy node-wot + Thing Description
Directory + broker MQTT) senza alcuna modifica lato server.

## Come funziona (parità con Android)

| Funzione | Android | iOS |
|---|---|---|
| UI | Fragments + Navigation component | SwiftUI (TabView + NavigationStack) |
| Stato | LiveData / ViewModel | `ObservableObject` / `@Published` |
| Consumo dashboard (`crwsns:dashboard`) | wot-servient Java, binding HTTP | `ConsumedThing` (URLSession) |
| Esposizione del telefono come Thing | `MqttProtocolServer` (Paho) | `MQTTExposureServer` (CocoaMQTT) |
| Registrazione TD nella directory | `Servient.register` (OkHttp PUT) | `ThingDirectoryClient` (URLSession PUT) |
| Posizione | FusedLocationProviderClient | CoreLocation (`LocationProvider`) |
| Ping ogni 10 s alla dashboard | `TimerTask` | `Task` async con sleep |
| Persistenza adesioni | SharedPreferences per-bearer | UserDefaults per-bearer |

### Protocollo MQTT (identico al servient Java)

Per ogni property `p` della Thing `smartphone_<uuid>`:

- il telefono si sottoscrive a `smartphone_<uuid>/properties/p/readAsk`;
- quando il proxy pubblica un messaggio vuoto su quel topic, il telefono
  legge il sensore e pubblica il JSON su
  `smartphone_<uuid>/properties/p/readAnswer` (il proxy attende max 10 s);
- nella Thing Description il form della property ha
  `href = mqtt://<broker>:<porta>/<topic readAsk>` e
  `"mqv:answerTopic"` con il topic di risposta.

Le campagne pull aggiungono una property con il nome uguale all'id della
campagna; `ping` è sempre presente.

## Requisiti

- macOS con **Xcode 15+**
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`
- Un dispositivo o simulatore con **iOS 16+**

## Setup

```bash
cd ios
xcodegen generate      # crea CrowdsensingWoT.xcodeproj
open CrowdsensingWoT.xcodeproj
```

Alla prima apertura Xcode risolve automaticamente la dipendenza Swift
Package [CocoaMQTT](https://github.com/emqx/CocoaMQTT). Seleziona il tuo
team di firma in *Signing & Capabilities* e avvia.

I valori di default di Thing Directory e broker MQTT sono gli stessi
hardcoded nell'app Android (`vz5nc4zqbrgzpn13.myfritz.net`); si possono
cambiare da **Profilo → Impostazioni** dentro l'app.

## Struttura del codice

```
CrowdsensingWoT/
├── App/                    # Entry point + AppViewModel (stato centrale)
├── Models/                 # Campaign, AppliedCampaign, User, SubmissionMode
├── WoT/                    # Livello Web of Things minimale
│   ├── ThingDescription    # parsing TD consumate
│   ├── ConsumedThing       # consumer HTTP (dashboard)
│   ├── ExposedDeviceThing  # modello della Thing del telefono + generazione TD
│   ├── MQTTExposureServer  # esposizione via MQTT (CocoaMQTT)
│   └── ThingDirectoryClient# fetch/register/unregister TD
├── Services/
│   ├── ServientService     # orchestratore (login, expose, ping, campagne)
│   ├── LocationProvider    # CoreLocation
│   └── SettingsStore       # UserDefaults
└── Views/                  # SwiftUI: Auth, Campagne, Le mie campagne, Profilo
```

Non è stato portato l'intero `wot-servient` Java (~12.600 righe): l'app usa
un livello WoT minimale che implementa solo le interazioni realmente usate
dal sistema (lettura property e invocazione azioni via HTTP, esposizione
property via MQTT, registrazione TD). Il comportamento osservabile dal
backend è lo stesso.

## Differenze e limitazioni note su iOS

1. **Niente foreground service.** Su Android un servizio in foreground con
   notifica persistente tiene vivi servient e connessione MQTT
   indefinitamente. iOS non ha un equivalente: l'app resta attiva in
   background **solo** grazie al background mode `location` mentre riceve
   aggiornamenti di posizione (richiede il permesso "Sempre"). Con l'app
   attiva o in background con localizzazione attiva, il modello pull
   funziona normalmente; se il sistema sospende l'app, il proxy vedrà il
   dispositivo offline (il pinger scade dopo 20 s) finché l'utente non la
   riapre. Per una soluzione production-ready servirebbe un ripensamento
   push-based (es. silent push notification), che richiederebbe modifiche
   al backend.
2. **Ripristino delle campagne pull al riavvio.** L'app iOS ri-espone le
   property delle campagne pull salvate localmente a ogni login (l'app
   Android le perdeva al riavvio del processo).
3. **Invio manuale.** Viene inviata la posizione reale corrente
   (`{latitude, longitude}`); l'app Android inviava un valore di test
   hardcoded (300, 200).
4. **HTTP in chiaro.** Il backend è raggiungibile solo via `http://`, quindi
   App Transport Security è disabilitata nell'Info.plist (chiave
   `NSAllowsArbitraryLoads`). Da rimuovere se il backend passa a HTTPS.
5. **Tipi di campagna.** Come su Android, solo le campagne di tipo
   `location`/`gps` hanno un'implementazione reale del sensore; `camera` e
   `temperature` sono mostrate ma non implementate (iOS non ha comunque un
   sensore di temperatura ambientale).
6. **LA-MQTT.** Il binding location-aware MQTT sperimentale non è stato
   portato: anche su Android è disabilitato (commentato in
   `ServientService.generateConfig`).

## Nota sul repository

L'app vive nella cartella `ios/` di questo repository per comodità di
sviluppo; può essere estratta in un repository dedicato
(es. `crowdsensing-wot-ios-client`) senza modifiche.
