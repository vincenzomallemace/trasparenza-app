/**
 * Gemini Vision Service
 * Uses Google Gemini to analyze product images and extract producer information
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');

class GeminiService {
  constructor() {
    this.genAI = null;
    this.model = null;
  }

  initialize() {
    if (!this.genAI) {
      const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
      if (!apiKey) {
        throw new Error('GEMINI_API_KEY or GOOGLE_API_KEY not found in environment');
      }
      this.genAI = new GoogleGenerativeAI(apiKey);
      // Use gemini-2.5-flash for image analysis (latest model)
      this.model = this.genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
    }
  }

  /**
   * Analyze product image and extract detailed producer information
   * @param {Buffer|string} imageData - Image buffer or base64 string
   * @returns {Promise<Object>} Product and producer information
   */
  async analyzeProduct(imageData) {
    this.initialize();

    // Convert to base64 if buffer
    let base64Image;
    if (Buffer.isBuffer(imageData)) {
      base64Image = imageData.toString('base64');
    } else if (typeof imageData === 'string') {
      base64Image = imageData.replace(/^data:image\/\w+;base64,/, '');
    } else {
      throw new Error('Invalid image data format');
    }

    const prompt = `Analizza questa immagine di un prodotto alimentare e fornisci le seguenti informazioni in formato JSON.

IMPORTANTE: Cerca di identificare il VERO PRODUTTORE del prodotto, non solo il marchio commerciale.
- Per prodotti a marchio del supermercato (es. "Conad", "Esselunga", "Coop"), cerca chi lo produce realmente
- Cerca indicazioni come "Prodotto da", "Fabbricato da", "Produttore", "Stabilimento" sull'etichetta
- Se non trovi il produttore sull'etichetta, usa la tua conoscenza per identificare chi produce realmente quel tipo di prodotto per quel marchio

Rispondi SOLO con un oggetto JSON valido, senza testo aggiuntivo:

{
  "productName": "nome completo del prodotto",
  "brandName": "marchio commerciale visibile",
  "producerName": "nome esatto dell'azienda che produce (es. 'Barilla G. e R. Fratelli S.p.A.')",
  "producerType": "produttore|confezionatore|distributore",
  "groupName": "gruppo industriale se diverso dal produttore (es. 'Gruppo Barilla')",
  "headquarterCountry": "paese sede del produttore",
  "headquarterCity": "città sede del produttore se nota",
  "productCategory": "categoria merceologica",
  "confidence": 0.0-1.0,
  "notes": "eventuali note o incertezze"
}`;

    try {
      const result = await this.model.generateContent([
        prompt,
        {
          inlineData: {
            mimeType: 'image/jpeg',
            data: base64Image
          }
        }
      ]);

      const response = await result.response;
      const text = response.text();

      console.log('🤖 Gemini raw response:', text);

      // Parse JSON from response
      const jsonMatch = text.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsed = JSON.parse(jsonMatch[0]);
        return {
          success: true,
          data: parsed
        };
      } else {
        throw new Error('No valid JSON in response');
      }
    } catch (error) {
      console.error('❌ Gemini analysis error:', error.message);
      return {
        success: false,
        error: error.message
      };
    }
  }
}

module.exports = new GeminiService();

