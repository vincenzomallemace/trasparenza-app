/**
 * Receipt Analysis Routes
 * POST /api/analyze-receipt  - OCR + product extraction from receipt
 * POST /api/sustainability-report - Full packaging + eco report for a receipt
 */

const express = require('express');
const multer = require('multer');
const receiptService = require('../services/receiptService');
const packagingResearchService = require('../services/packagingResearchService');
const sustainabilityReportService = require('../services/sustainabilityReportService');

const router = express.Router();

// Multer: accept images, PDFs, and videos up to 50MB
const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: { fileSize: 50 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    const allowed = [
      'image/jpeg',
      'image/png',
      'image/webp',
      'image/heic',
      'application/pdf',
      'video/mp4',
      'video/quicktime',
      'video/mpeg',
    ];
    if (allowed.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error(`Unsupported file type: ${file.mimetype}`));
    }
  },
});

/**
 * POST /api/analyze-receipt
 * Extract products from a receipt image/PDF/video or base64 data
 *
 * Accepts:
 *  - multipart/form-data with field "receipt" (file)
 *  - JSON body with "receiptBase64" and optional "mimeType"
 *
 * Returns: { storeName, receiptDate, products: [...], confidence }
 */
router.post('/analyze-receipt', upload.single('receipt'), async (req, res, next) => {
  try {
    let data;
    let mimeType;

    if (req.file) {
      data = req.file.buffer;
      mimeType = req.file.mimetype;
      console.log(`🧾 Analyzing receipt file: ${req.file.originalname} (${req.file.size} bytes, ${mimeType})`);
    } else if (req.body.receiptBase64) {
      const base64Str = req.body.receiptBase64.replace(/^data:[^;]+;base64,/, '');
      data = base64Str;
      mimeType = req.body.mimeType || 'image/jpeg';
      console.log(`🧾 Analyzing receipt base64 (mimeType: ${mimeType})`);
    } else {
      return res.status(400).json({
        error: { message: 'No receipt provided. Send file as multipart or base64 JSON.', code: 'MISSING_RECEIPT' },
      });
    }

    const result = await receiptService.analyzeReceipt(data, mimeType);

    if (!result.success) {
      return res.status(422).json({
        error: { message: 'Could not extract receipt data', code: 'RECEIPT_PARSE_ERROR' },
      });
    }

    res.json(result.data);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/sustainability-report
 * Full pipeline: from receipt data extract packaging info and generate eco report
 *
 * Accepts:
 *  - multipart/form-data with field "receipt" (file)
 *  - JSON body with "receiptBase64" and optional "mimeType"
 *  - JSON body with "receiptData" (already parsed, skip OCR)
 *
 * Returns: Full SustainabilityReport object
 */
router.post('/sustainability-report', upload.single('receipt'), async (req, res, next) => {
  try {
    let receiptData;

    // Option A: pre-parsed receipt data passed directly (skip OCR)
    if (req.body.receiptData) {
      try {
        receiptData =
          typeof req.body.receiptData === 'string'
            ? JSON.parse(req.body.receiptData)
            : req.body.receiptData;
        console.log(`📊 Using pre-parsed receipt data (${receiptData.products?.length || 0} products)`);
      } catch {
        return res.status(400).json({
          error: { message: 'Invalid receiptData JSON', code: 'INVALID_RECEIPT_DATA' },
        });
      }
    }
    // Option B: run full OCR pipeline
    else {
      let data;
      let mimeType;

      if (req.file) {
        data = req.file.buffer;
        mimeType = req.file.mimetype;
      } else if (req.body.receiptBase64) {
        data = req.body.receiptBase64.replace(/^data:[^;]+;base64,/, '');
        mimeType = req.body.mimeType || 'image/jpeg';
      } else {
        return res.status(400).json({
          error: { message: 'No receipt or receiptData provided.', code: 'MISSING_INPUT' },
        });
      }

      console.log(`🔄 Full pipeline: OCR + packaging research...`);
      const receiptResult = await receiptService.analyzeReceipt(data, mimeType);
      if (!receiptResult.success) {
        return res.status(422).json({
          error: { message: 'Could not extract receipt data', code: 'RECEIPT_PARSE_ERROR' },
        });
      }
      receiptData = receiptResult.data;
    }

    if (!receiptData.products || receiptData.products.length === 0) {
      return res.status(422).json({
        error: { message: 'No products found in receipt', code: 'NO_PRODUCTS' },
      });
    }

    console.log(`📦 Researching packaging for ${receiptData.products.length} products...`);
    const packagingData = await packagingResearchService.analyzePackagingBatch(
      receiptData.products.map((p) => ({
        name: p.name,
        brand: p.brand,
        category: p.category,
      }))
    );

    console.log(`📊 Generating sustainability report...`);
    const report = sustainabilityReportService.generateReport(receiptData, packagingData);

    res.json(report);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
