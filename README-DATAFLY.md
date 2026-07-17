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

## Stato e come procedere

- ⚠️ **Questo branch non è stato compilato**: l'ambiente di sviluppo remoto non
  dispone di Android SDK/Gradle. Il layer è scritto contro le API pubbliche del
  monorepo (verificate da 88 test lato piattaforma, simulatore incluso, che
  esercita esattamente le stesse chiamate).
- Passi suggeriti, in ordine:
  1. `./gradlew assembleDebug` e correzione di eventuali dettagli di compilazione;
  2. smoke manuale con lo stack su (`docker compose up` nel monorepo, emulatore
     con default `10.0.2.2`): login → registrazione device → lista campagne →
     join → invio manuale → esito con crediti → wallet;
  3. cablaggio UI seguendo la mappa sopra (i punti di intervento sono
     `ServientService`, `DashboardViewModel`, `JoinCampaignReviewDialog`,
     `AuthFragment`), sostituendo le invocazioni della vecchia Dashboard con la
     facciata `DataFlyIntegration`;
  4. schermata Wallet (nuova) su `DataFlyIntegration.wallet()`.
