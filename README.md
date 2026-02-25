# Trasparenza - Product Scanner App

App Android per scansionare prodotti da supermercato e ottenere informazioni sul produttore, gruppo aziendale e paese di origine.

## Architettura

```
├── android-app/          # App Android (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/com/trasparenza/app/
│   │   ├── data/         # Layer dati (API, Room, Repository)
│   │   ├── di/           # Dependency Injection (Hilt)
│   │   └── ui/           # UI Compose (Scanner, Saved)
│   └── gradle/           # Configurazione Gradle
│
└── backend/              # Backend Node.js
    └── src/
        ├── routes/       # Endpoint API
        ├── services/     # Google Vision, Search, Enricher
        └── utils/        # Utilities
```

## Setup Backend

### 1. Prerequisiti
- Node.js 18+
- Account Google Cloud con API abilitate:
  - Cloud Vision API
  - Custom Search JSON API
  - Knowledge Graph Search API

### 2. Configurazione API Google

1. Vai su [Google Cloud Console](https://console.cloud.google.com)
2. Crea un nuovo progetto o seleziona uno esistente
3. Abilita le API:
   - Cloud Vision API
   - Custom Search JSON API  
   - Knowledge Graph Search API
4. Crea credenziali:
   - Per Vision API: crea un Service Account e scarica il JSON
   - Per Search API: crea una API Key
5. Crea un Custom Search Engine su [Programmable Search Engine](https://programmablesearchengine.google.com)

### 3. Configurazione ambiente

```bash
cd backend
cp .env.example .env
```

Modifica `.env` con le tue credenziali:
```
GOOGLE_APPLICATION_CREDENTIALS=./path-to-service-account.json
GOOGLE_API_KEY=your-api-key
GOOGLE_CSE_ID=your-custom-search-engine-id
PORT=3000
```

### 4. Avvio

```bash
cd backend
npm install
npm start
```

Il server sarà disponibile su `http://localhost:3000`

### 5. Test API

```bash
curl -X POST http://localhost:3000/analyze-image \
  -F "image=@/path/to/product-image.jpg"
```

## Setup Android

### 1. Prerequisiti
- Android Studio Hedgehog (2023.1.1) o superiore
- JDK 17
- Android SDK 34

### 2. Configurazione

1. Apri `android-app` in Android Studio
2. Modifica l'URL del backend in `AppModule.kt`:
   ```kotlin
   // Per emulatore Android
   private const val BASE_URL = "http://10.0.2.2:3000/"
   
   // Per dispositivo fisico sulla stessa rete
   private const val BASE_URL = "http://YOUR_PC_IP:3000/"
   ```

### 3. Build e Run

1. Sincronizza Gradle
2. Connetti un dispositivo o avvia un emulatore
3. Clicca Run

## Funzionalità

### Scanner
- Apre la fotocamera in tempo reale
- Pulsante per catturare e analizzare l'immagine
- Bottom sheet con risultati:
  - Nome prodotto
  - Marca
  - Produttore/Gruppo aziendale
  - Paese di origine (con bandiera)
- Pulsante per salvare il prodotto

### Prodotti Salvati
- Lista dei prodotti salvati localmente (Room DB)
- Click per vedere dettagli
- Possibilità di eliminare

## API Response Format

```json
{
  "product": {
    "name": "Nutella",
    "brand": "Ferrero",
    "category": "Food"
  },
  "producer": {
    "name": "Ferrero SpA",
    "group": "Ferrero Group",
    "headquarters": {
      "country": "Italy",
      "region": "Piedmont"
    }
  },
  "confidence": 0.85,
  "sources": ["vision", "knowledge_graph", "web_search"]
}
```

## Estensioni Future

- [ ] Scansione barcode/QR code
- [ ] Cache risultati per prodotti già scansionati
- [ ] Modalità offline con database prodotti
- [ ] Filtri per paese/gruppo nella lista salvati
- [ ] Export dati in CSV
- [ ] Supporto iOS (React Native o Flutter)

