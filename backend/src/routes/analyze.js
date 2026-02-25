/**
 * API Routes for product analysis
 */

const express = require('express');
const multer = require('multer');
const productEnricher = require('../services/productEnricher');

const router = express.Router();

// Configure multer for image uploads
const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB max
  },
  fileFilter: (req, file, cb) => {
    // Accept only images
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Only image files are allowed'));
    }
  }
});

/**
 * POST /api/analyze-image
 * Analyzes a product image and returns structured product information
 * 
 * Request body:
 * - multipart/form-data with 'image' field
 * OR
 * - JSON with 'imageBase64' field
 * 
 * Response:
 * {
 *   productId: string | null,
 *   productName: string | null,
 *   brandName: string | null,
 *   producerName: string | null,
 *   groupName: string | null,
 *   headquarterCountry: string | null,
 *   headquarterRegion: string | null,
 *   sourceUrls: string[],
 *   confidence: number
 * }
 */
router.post('/analyze-image', upload.single('image'), async (req, res, next) => {
  try {
    let imageData;

    // Handle multipart upload
    if (req.file) {
      imageData = req.file.buffer;
    } 
    // Handle base64 JSON upload
    else if (req.body.imageBase64) {
      // Remove data URL prefix if present
      const base64Data = req.body.imageBase64.replace(/^data:image\/\w+;base64,/, '');
      imageData = base64Data;
    } else {
      return res.status(400).json({
        error: {
          message: 'No image provided. Send image as multipart or base64 JSON.',
          code: 'MISSING_IMAGE'
        }
      });
    }

    console.log(`📸 Analyzing image (${req.file?.size || 'base64'} bytes)...`);

    // Analyze and enrich product info
    const result = await productEnricher.analyzeAndEnrich(imageData);

    console.log(`✅ Analysis complete. Brand: ${result.brandName || 'unknown'}`);

    res.json(result);

  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/test
 * Simple test endpoint
 */
router.get('/test', (req, res) => {
  res.json({
    message: 'Trasparenza API is working!',
    endpoints: {
      'POST /api/analyze-image': 'Analyze product image'
    }
  });
});

module.exports = router;

