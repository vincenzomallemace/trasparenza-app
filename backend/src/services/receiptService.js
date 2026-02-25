/**
 * Receipt Service
 * Uses Gemini Vision to extract product list from a receipt (image, PDF, video frame)
 */

const { GoogleGenerativeAI } = require('@google/generative-ai');
const pdfParse = require('pdf-parse');

class ReceiptService {
  constructor() {
    this.genAI = null;
    this.model = null;
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
   * Analyze a receipt and return structured product list
   * @param {Buffer|string} data - Image buffer, base64 string, or PDF buffer/base64
   * @param {string} mimeType - 'image/jpeg'|'image/png'|'application/pdf'|'video/mp4'
   * @returns {Promise<ReceiptAnalysisResult>}
   */
  async analyzeReceipt(data, mimeType = 'image/jpeg') {
    this.initialize();

    // Normalize data to base64 string regardless of input type
    let base64Data;
    if (Buffer.isBuffer(data)) {
      base64Data = data.toString('base64');
    } else if (typeof data === 'string') {
      // Strip data URI prefix if present (e.g. "data:application/pdf;base64,...")
      base64Data = data.replace(/^data:[^;]+;base64,/, '');
    } else {
      throw new Error('Invalid data format');
    }

    // Gemini 2.5 Flash supports image/jpeg, image/png, application/pdf as inlineData.
    // For video frames we already extract a JPEG frame before calling this method.
    const geminiMimeType = mimeType.startsWith('video') ? 'image/jpeg' : mimeType;

    const prompt = this._buildReceiptPrompt();

    try {
      const result = await this.model.generateContent([
        prompt,
        {
          inlineData: {
            mimeType: geminiMimeType,
            data: base64Data,
          },
        },
      ]);

      const text = result.response.text();
      console.log('🧾 Gemini receipt raw response:', text.substring(0, 300));
      return this._parseReceiptResponse(text);
    } catch (err) {
      console.error('❌ Gemini receipt analysis failed:', err);
      throw err;
    }
  }

  _buildReceiptPrompt() {
    return `Sei un assistente che analizza scontrini. Analizza questa immagine di uno scontrino e fornisci le informazioni in formato JSON.

Estrai tutti i prodotti acquistati con il massimo dettaglio possibile.

${this._buildReceiptJsonSchema()}`;
  }

  _buildReceiptJsonSchema() {
    return `Rispondi SOLO con un oggetto JSON valido, senza testo aggiuntivo:

{
  "storeName": "nome del supermercato/negozio",
  "storeAddress": "indirizzo del negozio se visibile",
  "receiptDate": "data in formato ISO 8601 (YYYY-MM-DD)",
  "receiptTime": "ora in formato HH:MM",
  "receiptNumber": "numero scontrino se visibile",
  "totalAmount": 0.00,
  "currency": "EUR",
  "products": [
    {
      "name": "nome completo del prodotto come appare sullo scontrino",
      "brand": "marca se identificabile",
      "category": "categoria merceologica (es: latticini, pasta, bevande, carne, verdura, snack, detersivi, cura personale)",
      "quantity": 1,
      "unit": "pz|kg|g|l|ml",
      "pricePerUnit": 0.00,
      "totalPrice": 0.00,
      "barcode": "se visibile",
      "isOrganic": false,
      "notes": "eventuali note"
    }
  ],
  "paymentMethod": "contanti|carta|altro",
  "vatNumber": "p.iva del negozio se visibile",
  "confidence": 0.0
}`;
  }

  _parseReceiptResponse(text) {
    // Extract JSON from response
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      throw new Error('No valid JSON in Gemini receipt response');
    }

    try {
      const parsed = JSON.parse(jsonMatch[0]);
      return {
        success: true,
        data: {
          storeName: parsed.storeName || null,
          storeAddress: parsed.storeAddress || null,
          receiptDate: parsed.receiptDate || null,
          receiptTime: parsed.receiptTime || null,
          receiptNumber: parsed.receiptNumber || null,
          totalAmount: parsed.totalAmount || null,
          currency: parsed.currency || 'EUR',
          products: (parsed.products || []).map((p) => ({
            name: p.name || '',
            brand: p.brand || null,
            category: p.category || null,
            quantity: p.quantity || 1,
            unit: p.unit || 'pz',
            pricePerUnit: p.pricePerUnit || null,
            totalPrice: p.totalPrice || null,
            barcode: p.barcode || null,
            isOrganic: p.isOrganic || false,
            notes: p.notes || null,
          })),
          paymentMethod: parsed.paymentMethod || null,
          vatNumber: parsed.vatNumber || null,
          confidence: parsed.confidence || 0.7,
        },
      };
    } catch (err) {
      throw new Error(`Failed to parse receipt JSON: ${err.message}`);
    }
  }
}

module.exports = new ReceiptService();
