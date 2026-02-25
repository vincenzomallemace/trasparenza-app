/**
 * Packaging Research Service
 * For each product, searches the web and uses Gemini to extract packaging information
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');
const searchService = require('./searchService');

class PackagingResearchService {
  constructor() {
    this.genAI = null;
    this.model = null;
    // In-memory cache to avoid duplicate API calls
    this._cache = new Map();
  }

  initialize() {
    if (!this.genAI) {
      const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
      if (!apiKey) throw new Error('GEMINI_API_KEY not found');
      this.genAI = new GoogleGenerativeAI(apiKey);
      this.model = this.genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
    }
  }

  /**
   * Analyze packaging for a list of products (with concurrency limit)
   * @param {Array<{name: string, brand: string|null, category: string|null}>} products
   * @returns {Promise<PackagingInfo[]>}
   */
  async analyzePackagingBatch(products) {
    const CONCURRENCY = 3;
    const results = [];

    for (let i = 0; i < products.length; i += CONCURRENCY) {
      const batch = products.slice(i, i + CONCURRENCY);
      const batchResults = await Promise.all(
        batch.map((p) => this.analyzeProductPackaging(p))
      );
      results.push(...batchResults);
    }

    return results;
  }

  /**
   * Analyze packaging for a single product
   * @param {{ name: string, brand?: string, category?: string }} product
   * @returns {Promise<PackagingInfo>}
   */
  async analyzeProductPackaging(product) {
    this.initialize();

    const cacheKey = `${product.brand || ''}_${product.name}`.toLowerCase();
    if (this._cache.has(cacheKey)) {
      console.log(`📦 Cache hit for: ${product.name}`);
      return this._cache.get(cacheKey);
    }

    console.log(`🔍 Researching packaging for: ${product.name}`);

    // Build search queries
    const queries = this._buildSearchQueries(product);
    let searchContext = '';

    // Try to get web context via search
    for (const query of queries) {
      try {
        const searchResults = await searchService.searchBrandInfo(query);
        if (searchResults && searchResults.items && searchResults.items.length > 0) {
          searchContext += searchResults.items
            .slice(0, 3)
            .map((r) => `${r.title}: ${r.snippet}`)
            .join('\n');
          break;
        }
      } catch (err) {
        console.warn(`⚠️ Search failed for "${query}": ${err.message}`);
      }
    }

    // Use Gemini to analyze packaging
    const packagingInfo = await this._askGeminiPackaging(product, searchContext);

    this._cache.set(cacheKey, packagingInfo);
    return packagingInfo;
  }

  _buildSearchQueries(product) {
    const base = product.brand
      ? `${product.brand} ${product.name}`
      : product.name;
    return [
      `${base} packaging materiale imballaggio plastica`,
      `${base} packaging material plastic recyclable`,
      `${base} imballaggio sostenibile riciclabile`,
    ];
  }

  async _askGeminiPackaging(product, searchContext) {
    const prompt = `Sei un esperto di sostenibilità e packaging alimentare.

Analizza il packaging tipico del seguente prodotto e fornisci le stime di impatto ambientale.

PRODOTTO: ${product.name}
MARCA: ${product.brand || 'non specificata'}
CATEGORIA: ${product.category || 'non specificata'}

${searchContext ? `CONTESTO WEB:\n${searchContext}\n` : ''}

Basandoti sulla tua conoscenza dei materiali di packaging tipici per questa tipologia di prodotto, fornisci una stima dettagliata.

Rispondi SOLO con JSON valido:
{
  "primaryPackagingMaterial": "plastica|carta|vetro|alluminio|cartone|tetrapack|misto",
  "secondaryPackagingMaterial": "materiale imballaggio secondario se presente",
  "packagingDescription": "descrizione breve del packaging",
  "totalPackagingWeightGrams": 0,
  "plasticGrams": 0,
  "recyclablePlasticGrams": 0,
  "nonRecyclablePlasticGrams": 0,
  "cardboardGrams": 0,
  "glassGrams": 0,
  "aluminumGrams": 0,
  "otherMaterialGrams": 0,
  "isFullyRecyclable": false,
  "recyclabilityScore": 0,
  "recyclabilityLabel": "ottimo|buono|sufficiente|scarso|pessimo",
  "plasticTypes": ["PET", "HDPE", "PP"],
  "recyclingInstructions": "come smaltire correttamente",
  "co2PackagingKg": 0.0,
  "certifications": ["FSC", "RecyClass"],
  "environmentalNotes": "note sull'impatto ambientale",
  "dataConfidence": 0.0,
  "dataSource": "gemini_knowledge|web_search|combined"
}

SCALA recyclabilityScore: 0=non riciclabile, 100=completamente riciclabile
I valori in grammi sono STIME tipiche per un singolo pezzo/confezione del prodotto.`;

    try {
      const result = await this.model.generateContent([prompt]);
      const text = result.response.text();
      console.log(`✅ Packaging analysis for "${product.name}":`, text.substring(0, 200));

      const jsonMatch = text.match(/\{[\s\S]*\}/);
      if (!jsonMatch) throw new Error('No JSON in response');

      const parsed = JSON.parse(jsonMatch[0]);
      return {
        productName: product.name,
        brand: product.brand || null,
        category: product.category || null,
        primaryPackagingMaterial: parsed.primaryPackagingMaterial || 'misto',
        secondaryPackagingMaterial: parsed.secondaryPackagingMaterial || null,
        packagingDescription: parsed.packagingDescription || null,
        totalPackagingWeightGrams: parsed.totalPackagingWeightGrams || 0,
        plasticGrams: parsed.plasticGrams || 0,
        recyclablePlasticGrams: parsed.recyclablePlasticGrams || 0,
        nonRecyclablePlasticGrams: parsed.nonRecyclablePlasticGrams || 0,
        cardboardGrams: parsed.cardboardGrams || 0,
        glassGrams: parsed.glassGrams || 0,
        aluminumGrams: parsed.aluminumGrams || 0,
        otherMaterialGrams: parsed.otherMaterialGrams || 0,
        isFullyRecyclable: parsed.isFullyRecyclable || false,
        recyclabilityScore: parsed.recyclabilityScore || 0,
        recyclabilityLabel: parsed.recyclabilityLabel || 'sconosciuto',
        plasticTypes: parsed.plasticTypes || [],
        recyclingInstructions: parsed.recyclingInstructions || null,
        co2PackagingKg: parsed.co2PackagingKg || 0,
        certifications: parsed.certifications || [],
        environmentalNotes: parsed.environmentalNotes || null,
        dataConfidence: parsed.dataConfidence || 0.5,
        dataSource: parsed.dataSource || 'gemini_knowledge',
      };
    } catch (err) {
      console.error(`❌ Gemini packaging failed for "${product.name}":`, err.message);
      // Return fallback with zeros
      return {
        productName: product.name,
        brand: product.brand || null,
        category: product.category || null,
        primaryPackagingMaterial: 'sconosciuto',
        secondaryPackagingMaterial: null,
        packagingDescription: null,
        totalPackagingWeightGrams: 0,
        plasticGrams: 0,
        recyclablePlasticGrams: 0,
        nonRecyclablePlasticGrams: 0,
        cardboardGrams: 0,
        glassGrams: 0,
        aluminumGrams: 0,
        otherMaterialGrams: 0,
        isFullyRecyclable: false,
        recyclabilityScore: 0,
        recyclabilityLabel: 'sconosciuto',
        plasticTypes: [],
        recyclingInstructions: null,
        co2PackagingKg: 0,
        certifications: [],
        environmentalNotes: 'Analisi non disponibile',
        dataConfidence: 0,
        dataSource: 'error',
      };
    }
  }
}

module.exports = new PackagingResearchService();
