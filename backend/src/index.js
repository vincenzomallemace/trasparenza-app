/**
 * Backend server for "Trasparenza" app
 * Handles product image analysis using Google Cloud Vision and Custom Search APIs
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const analyzeRoutes = require('./routes/analyze');
const receiptRoutes = require('./routes/receipt');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request logging middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`\n📥 [${timestamp}] ${req.method} ${req.url}`);
  console.log(`   Headers: ${JSON.stringify({
    'content-type': req.headers['content-type'],
    'user-agent': req.headers['user-agent']?.substring(0, 50),
    'content-length': req.headers['content-length']
  })}`);

  const startTime = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - startTime;
    console.log(`📤 [${timestamp}] Response: ${res.statusCode} (${duration}ms)`);
  });
  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    version: '1.0.0'
  });
});

// API Routes
app.use('/api', analyzeRoutes);
app.use('/api', receiptRoutes);

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    error: {
      message: err.message || 'Internal Server Error',
      code: err.code || 'UNKNOWN_ERROR'
    }
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: {
      message: 'Endpoint not found',
      code: 'NOT_FOUND'
    }
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Trasparenza Backend running on port ${PORT}`);
  console.log(`📍 Health check: http://localhost:${PORT}/health`);
  console.log(`📍 Analyze endpoint: POST http://localhost:${PORT}/api/analyze-image`);
  console.log(`📍 Receipt endpoint: POST http://localhost:${PORT}/api/analyze-receipt`);
  console.log(`📍 Report endpoint: POST http://localhost:${PORT}/api/sustainability-report`);
});

module.exports = app;

