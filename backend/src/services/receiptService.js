/**
 * Receipt Service
 * Two-step architecture:
 *   Step 1 — Static OCR: pdf-parse (PDF) or Gemini Vision text extraction (image)
 *   Step 2 — Gemini text-only: structured JSON parsing of the extracted plain text
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
   * Analyze a receipt and return structured product list.
   * @param {Buffer|string} data - base64 string or Buffer
   * @param {string} mimeType - 'image/jpeg'|'image/png'|'application/pdf'|'video/mp4'
   */
  async analyzeReceipt(data, mimeType = 'image/jpeg') {
    this.initialize();

    // --- Normalize to Buffer ---
    let buffer;
    if (Buffer.isBuffer(data)) {
      buffer = data;
    } else if (typeof data === 'string') {
      const raw = data.replace(/^data:[^;]+;base64,/, '');
      buffer = Buffer.from(raw, 'base64');
    } else {
      throw new Error('Invalid data format: expected Buffer or base64 string');
    }

    // STEP 1: Static OCR → plain text
    const rawText = await this._extractText(buffer, mimeType);
    console.log('📝 OCR extracted text (first 500):', rawText.substring(0, 500));

    if (!rawText || rawText.trim().length < 10) {
      throw new Error('OCR produced no readable text from the receipt');
    }

    // STEP 2: Gemini text-only → structured JSON
    return this._parseTextWithGemini(rawText);
  }

  /**
   * STEP 1 — Extract plain text from the receipt.
   * PDF  → pdf-parse (fast, deterministic, no AI credits)
   * Image/Video frame → Gemini Vision OCR-only prompt (ask only for raw text)
   */
  async _extractText(buffer, mimeType) {
    if (mimeType === 'application/pdf') {
      return this._ocrPdf(buffer);
    }
    // Images (jpeg, png) and video frames
    const imageMime = mimeType.startsWith('video') ? 'image/jpeg' : mimeType;
    return this._ocrImage(buffer, imageMime);
  }

  async _ocrPdf(buffer) {
    try {
      const result = await pdfParse(buffer);
      const text = result.text || '';
      console.log(`📄 pdf-parse extracted ${text.length} chars`);
      return text;
    } catch (err) {
      console.warn('⚠️ pdf-parse failed:', err.message);
      // Last-resort: decode buffer as UTF-8 (works for text-based receipts)
      return buffer.toString('utf-8').replace(/[^\x20-\x7E\xA0-\xFF\n\r\t]/g, ' ');
    }
  }

  async _ocrImage(buffer, mimeType) {
    const base64 = buffer.toString('base64');
    const ocrPrompt =
      'You are an OCR engine. Transcribe ALL text visible in this receipt image ' +
      'exactly as it appears, line by line. Do NOT interpret or summarize — just output the raw text.';
    try {
      const result = await this.model.generateContent([
        ocrPrompt,
        { inlineData: { mimeType, data: base64 } },
      ]);
      return result.response.text();
    } catch (err) {
      console.error('❌ Gemini OCR step failed:', err.message);
      throw err;
    }
  }

  /**
   * STEP 2 — Parse plain text into structured receipt JSON using Gemini (text-only).
   */
  async _parseTextWithGemini(rawText) {
    const prompt = `Sei un assistente che analizza scontrini della spesa italiani.
Leggi il seguente testo estratto da uno scontrino e restituisci le informazioni strutturate.

TESTO SCONTRINO:
\`\`\`
${rawText}
\`\`\`

${this._buildReceiptJsonSchema()}`;

    try {
      const result = await this.model.generateContent([prompt]);
      const text = result.response.text();
      console.log('🧾 Gemini parse raw response (first 400):', text.substring(0, 400));
      return this._parseReceiptResponse(text);
    } catch (err) {
      console.error('❌ Gemini parse step failed:', err);
      throw err;
    }
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
    // Gemini sometimes wraps JSON in ```json ... ``` — strip those first
    let cleaned = text
      .replace(/```json\s*/gi, '')
      .replace(/```\s*/g, '')
      .trim();

    // Extract the outermost JSON object
    const jsonMatch = cleaned.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      throw new Error(`No JSON object found in Gemini response. Raw: ${text.substring(0, 200)}`);
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
