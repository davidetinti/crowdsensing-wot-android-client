# Evoluzione Data Fly — app worker

Questo branch introduce il layer **`datafly/`** (package
`com.example.crowdsensingwotandroidapp.datafly`) che collega l'app al
marketplace **Data Fly** (monorepo
[`data-explorer-wot`](https://github.com/davidetinti/data-explorer-wot)),
evoluzione dell'architettura di questa tesi.

## Cosa contiene il layer (autosufficiente, nessuna nuova dipendenza)

| Classe | Ruolo |
|---|---|
| `DataFlyConfig` | endpoint (Core, TD del Master) + persistenza token in SharedPreferences |
| `CoreApiClient` | identità JWT sul Core (signup/login/logout/refresh **con rotazione**, retry singolo su 401) e **wallet** (`GET /wallet`: available/pending/movimenti) |
| `SensorDiscovery` | auto-discovery: `hasSystemFeature(FEATURE_LOCATION_GPS)` → `core/location@1.0.0` (stessi hint di `data_specs.discovery` del registry; `SensorManager` pronto per le prossime spec) |
| `DeviceTdFactory` | TD del device (id `smartphone_<uuid>` come in tesi) **con annotazioni** `@type`/`dfy:spec` che la rendono matchabile dal Master |
| `MasterClient` | consuma la TD `datafly-master` e invoca le azioni **dalle form** (vero flusso WoT), busta `{ok|error}` come in tesi |
| `DataFlyIntegration` | facciata del flusso completo: login → discovery → registerDevice → listCampaigns → join → submitData → ping → wallet |

Usa solo Jackson (già in `build.gradle`) e `HttpURLConnection`: nessuna
dipendenza aggiunta. Tutte le chiamate sono bloccanti, da eseguire
sull'executor già usato da `ServientService`.

## Mappa di migrazione (vecchia Dashboard → Data Fly)

| Oggi (tesi) | Dove | Diventa |
|---|---|---|
| azione `registerNewUser` / `loginUser` | `ServientService.invokeAction(...)` da Auth | `CoreApiClient.signup/login` (JWT; le credenziali non transitano più su affordances WoT) |
| azione `retrieveUserData` (punti) | `DashboardViewModel` | `DataFlyIntegration.wallet()` (available/pending in centesimi di credito) |
| property `campaigns` (catalogo completo) | `ServientService.readProperty` | `DataFlyIntegration.listCampaigns(lat, lon)` (già filtrate per capability e area; include `netPerSubmissionCents` = stima crediti) |
| `applyToCampaignPull` (lettura del Master) | `JoinCampaignReviewDialog` → service | `joinCampaign(id, "push")` — **risponde 409 fino a M7** (canale protetto) |
| `applyToCampaignPush` + invii | idem | `joinCampaign(id, "pull_auto" \| "pull_manual")` |
| `sendPushCampaignData` | `ServientService` | `DataFlyIntegration.submitLocation(...)` — la risposta contiene **esito validazione, score e crediti** (M5) |
| `wot.register(directoryUrl, exposedDevice)` (TDD diretta) | `ServientService` | `DataFlyIntegration.registerDevice()` (la TD passa dal Master: validazione + indice capability; la TDD la aggiorna il Master) |
| `ping` ogni 10 s sulla Dashboard | `ServientService` TimerTask | invariato nel periodo: `DataFlyIntegration.ping()` |
| esposizione del servient device (MQTT) | `ServientService` | resta com'è: si riaggancia in **M7** con credenziali per-device e ACL |

## Cablaggio UI (fatto)

La UI esistente **non è stata toccata**: `ServientService` è stato riscritto
mantenendo firma per firma l'API pubblica usata da `AuthFragment` e
`DashboardViewModel` e delegando tutto al layer `datafly/`. In dettaglio:

| Metodo (firma invariata) | Ora fa |
|---|---|
| `register/login(email, password)` | `CoreApiClient.signup/login` → salva email e token → `registerDevice()` sul Master → avvia il ping (10 s). Restituisce **l'email** come "bearer": è stabile tra sessioni (il JWT ruota a ogni refresh), così le adesioni salvate in SharedPreferences col bearer come namespace sopravvivono |
| `fetchData(bearer)` | `wallet()`: punti = (crediti disponibili + in vesting) / 100 |
| `fetchCampaigns()` | `listCampaigns(lat, lon)` con l'ultima posizione nota (best effort) e mappatura sulle chiavi legacy di `Campaign(HashMap)`: `points` = crediti netti stimati, `submission_required` = target della campagna, `organization` = nome del committente, `area` → `{circle, radius, center_lat, center_lng}` |
| `applyToCampaign(..., mode, interval)` | `joinCampaign` con mappa modi: `AUTOMATIC→"push"` (errore fino a M7), `AUTO_WITH_PREF→"pull_auto"`, `MANUAL→"pull_manual"`; `interval` resta preferenza locale |
| `sendManualData(...)` | `submitLocation(lat, lon, accuracy)`; accettata → `{ok, remaining, score, netCents}` (la UI usa `remaining==0` per il completamento), respinta → `{ok:false, error:<motivi validazione>}` |
| `leaveCampaign` / `logout` | `leaveCampaign` sul Master / revoca del refresh token sul Core + stop del ping |

Modifiche minime fuori dal service: `LocationSubmission` ora porta anche
l'`accuracy` (richiesta dalla spec `core/location@1.0.0`; il costruttore
vecchio resta valido) e `CampaignManualSendDialog` la valorizza dal fix GPS.
Il vecchio stack wot-servient (esposizione MQTT del device, TDD diretta) non è
più nel percorso critico: si riaggancia in **M7** con credenziali per-device.

## Canale push M7 (piattaforma pronta, client da cablare)

Dalla M7 della piattaforma, `registerDevice` restituisce anche le
**credenziali MQTT per-device** (broker dynsec, ACL deny-default): il layer le
salva già in `DataFlyConfig` (`saveMqttCredentials`, ruotate a ogni
registrazione). Per attivare la modalità `AUTOMATIC` (→ `push`) resta da
scrivere il client MQTT dell'app: connessione a `getMqttUrl()` con
username/password salvate e will `LWT/{deviceId}="offline"` (+ publish
`"online"` alla connect), subscribe di `ASK/{deviceId}`, e per ogni ASK una
publish su `ANSWER/{deviceId}` con `{jti (riflesso), payload{lat,lon,accuracy,
capturedAt}}`. Il jti è monouso con scadenza 30 s: rispondere subito e una
volta sola. Riferimento lato piattaforma: `apps/master/src/mqtt/ask-answer.ts`
e il simulatore `apps/device-simulator/src/push-device.ts` (stesso flusso in
TypeScript).

## Stato e come procedere

- ⚠️ **Questo branch non è stato compilato**: l'ambiente di sviluppo remoto non
  dispone di Android SDK/Gradle. Il layer è scritto contro le API pubbliche del
  monorepo (verificate dai test lato piattaforma, simulatore incluso, che
  esercita esattamente le stesse chiamate).
- Passi suggeriti, in ordine:
  1. `./gradlew assembleDebug` e correzione di eventuali dettagli di compilazione;
  2. smoke manuale con lo stack su (`docker compose up` nel monorepo, emulatore
     con default `10.0.2.2`): login → registrazione device → lista campagne →
     join (`MANUAL` o `AUTO_WITH_PREF`) → invio manuale → esito con crediti →
     punti aggiornati in dashboard;
  3. schermata Wallet dedicata (nuova) su `DataFlyIntegration.wallet()` per
     mostrare disponibile/in-vesting e lo storico movimenti.
