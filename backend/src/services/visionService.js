/**
 * Google Cloud Vision API Service
 * Analyzes product images to extract labels, text, and web entities
 */

const vision = require('@google-cloud/vision');

class VisionService {
  constructor() {
    // Initialize Vision client
    // Uses GOOGLE_APPLICATION_CREDENTIALS env var for authentication
    this.client = new vision.ImageAnnotatorClient();
  }

  /**
   * Analyze an image and extract product information
   * @param {Buffer|string} imageData - Image buffer or base64 string
   * @returns {Promise<Object>} Analysis results with labels, text, and web entities
   */
  async analyzeImage(imageData) {
    try {
      // Prepare image request
      const image = Buffer.isBuffer(imageData) 
        ? imageData.toString('base64') 
        : imageData;

      const request = {
        image: { content: image },
        features: [
          { type: 'LABEL_DETECTION', maxResults: 15 },
          { type: 'TEXT_DETECTION' },
          { type: 'WEB_DETECTION', maxResults: 10 },
          { type: 'LOGO_DETECTION', maxResults: 5 }
        ]
      };

      const [result] = await this.client.annotateImage(request);
      return this.parseResults(result);

    } catch (error) {
      console.error('Vision API error:', error);
      throw new Error(`Vision analysis failed: ${error.message}`);
    }
  }

  /**
   * Parse Vision API results into a structured format
   */
  parseResults(result) {
    const parsed = {
      labels: [],
      detectedText: '',
      webEntities: [],
      logos: [],
      bestGuessLabels: [],
      possibleProductNames: []
    };

    // Extract labels
    if (result.labelAnnotations) {
      parsed.labels = result.labelAnnotations.map(label => ({
        description: label.description,
        score: label.score
      }));
    }

    // Extract text (OCR)
    if (result.textAnnotations && result.textAnnotations.length > 0) {
      // First annotation contains the full text
      parsed.detectedText = result.textAnnotations[0].description || '';
    }

    // Extract web entities
    if (result.webDetection) {
      const webDetection = result.webDetection;
      
      if (webDetection.webEntities) {
        parsed.webEntities = webDetection.webEntities
          .filter(entity => entity.description)
          .map(entity => ({
            description: entity.description,
            score: entity.score || 0
          }))
          .sort((a, b) => b.score - a.score);
      }

      if (webDetection.bestGuessLabels) {
        parsed.bestGuessLabels = webDetection.bestGuessLabels.map(l => l.label);
      }
    }

    // Extract logos
    if (result.logoAnnotations) {
      parsed.logos = result.logoAnnotations.map(logo => ({
        description: logo.description,
        score: logo.score
      }));
    }

    // Build possible product names from various sources
    parsed.possibleProductNames = this.extractProductNames(parsed);

    return parsed;
  }

  /**
   * Extract possible product names from analysis results
   */
  extractProductNames(parsed) {
    const names = new Set();
    
    // Add logos as potential brand names
    parsed.logos.forEach(logo => names.add(logo.description));
    
    // Add high-confidence web entities
    parsed.webEntities
      .filter(e => e.score > 0.5)
      .slice(0, 5)
      .forEach(e => names.add(e.description));
    
    // Add best guess labels
    parsed.bestGuessLabels.forEach(label => names.add(label));

    return Array.from(names);
  }
}

module.exports = new VisionService();

