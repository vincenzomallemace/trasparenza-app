/**
 * Product Enricher Service
 * Uses Gemini AI for intelligent product analysis
 * Falls back to Vision API + Search if Gemini fails
 */

const visionService = require('./visionService');
const searchService = require('./searchService');
const geminiService = require('./geminiService');

class ProductEnricher {
  /**
   * Analyze a product image and enrich with company information
   * Primary: Gemini AI analysis
   * Fallback: Vision API + cascading search
   */
  async analyzeAndEnrich(imageData) {
    // TRY GEMINI FIRST (most accurate)
    console.log('🤖 Trying Gemini AI analysis...');
    const geminiResult = await geminiService.analyzeProduct(imageData);

    if (geminiResult.success && geminiResult.data) {
      console.log('✅ Gemini analysis successful!');
      const data = geminiResult.data;

      // Enrich with additional search if needed
      let enrichedData = { ...data };

      // If we have a producer, search for more details
      if (data.producerName && !data.headquarterCountry) {
        console.log('🔍 Enriching producer info...');
        const companyInfo = await this.findCompanyDetails(data.producerName, data.productName);
        enrichedData = {
          ...enrichedData,
          headquarterCountry: data.headquarterCountry || companyInfo.headquarterCountry,
          headquarterCity: data.headquarterCity || companyInfo.headquarterCity,
          groupName: data.groupName || companyInfo.groupName,
          sourceUrls: companyInfo.sourceUrls || []
        };
      }

      return this.formatGeminiResult(enrichedData);
    }

    // FALLBACK: Vision API + Search
    console.log('⚠️ Gemini failed, falling back to Vision API...');
    return this.fallbackAnalysis(imageData);
  }

  /**
   * Format Gemini result to match expected output structure
   */
  formatGeminiResult(data) {
    return {
      productId: null,
      productName: data.productName || null,
      brandName: data.brandName || null,
      producerName: data.producerName || null,
      producerType: data.producerType || null,
      groupName: data.groupName || null,
      headquarterCountry: data.headquarterCountry || null,
      headquarterCity: data.headquarterCity || null,
      productCategory: data.productCategory || null,
      sourceUrls: data.sourceUrls || [],
      confidence: data.confidence || 0.8,
      notes: data.notes || null,
      rawData: {
        source: 'gemini',
        originalResponse: data
      }
    };
  }

  /**
   * Fallback analysis using Vision API + Search
   */
  async fallbackAnalysis(imageData) {
    // STEP 1: Analyze image with Vision API to identify product
    console.log('📸 Step 1: Analyzing image with Vision API...');
    const visionResults = await visionService.analyzeImage(imageData);

    const productName = this.identifyProduct(visionResults);
    console.log('🏷️ Identified product:', productName);

    // STEP 2: Search for product pages on distributor sites to find the label info
    console.log('📦 Step 2: Searching product details on distributor sites...');
    const producerName = await this.findProducerFromProductPages(productName, visionResults);
    console.log('🏭 Found producer:', producerName);

    // STEP 3: Search for producer/company details
    console.log('🏢 Step 3: Searching producer company details...');
    const companyInfo = await this.findCompanyDetails(producerName, productName);
    console.log('📊 Company info:', JSON.stringify(companyInfo, null, 2));

    // STEP 4: Build final result
    return this.buildFinalResult(visionResults, productName, producerName, companyInfo);
  }

  /**
   * Identify the product from vision results
   */
  identifyProduct(visionResults) {
    // Priority 1: Logo detected
    if (visionResults.logos.length > 0) {
      const logoName = visionResults.logos[0].description;
      // Combine with text if available
      const firstTextLine = visionResults.detectedText?.split('\n')[0]?.trim();
      if (firstTextLine && firstTextLine.length > 2 && firstTextLine.length < 50) {
        return `${logoName} ${firstTextLine}`;
      }
      return logoName;
    }

    // Priority 2: Best guess from web detection
    if (visionResults.bestGuessLabels.length > 0) {
      return visionResults.bestGuessLabels[0];
    }

    // Priority 3: Top web entity
    if (visionResults.webEntities.length > 0) {
      return visionResults.webEntities[0].description;
    }

    // Priority 4: First line of OCR text
    if (visionResults.detectedText) {
      const firstLine = visionResults.detectedText.split('\n')[0].trim();
      if (firstLine.length > 2 && firstLine.length < 50) {
        return firstLine;
      }
    }

    return '';
  }

  /**
   * STEP 2: Search for product on distributor sites and extract producer
   */
  async findProducerFromProductPages(productName, visionResults) {
    if (!productName) return null;

    // Search on distributor/supermarket sites for product details
    const distributorSites = [
      'site:esselunga.it OR site:conad.it OR site:coop.it',
      'site:carrefour.it OR site:amazon.it OR site:auchan.it',
      'site:eurospin.it OR site:lidl.it OR site:md-discount.it'
    ];

    let producerName = null;

    // Try searching on distributor sites first
    for (const sites of distributorSites) {
      const query = `"${productName}" ${sites} prodotto da OR produttore OR fabbricato`;
      console.log('🔍 Searching:', query);

      const results = await searchService.searchBrandInfo(query);

      // Try to extract producer from snippets
      producerName = this.extractProducerFromSnippets(results.snippets);
      if (producerName) {
        console.log('✅ Found producer from distributor site:', producerName);
        return producerName;
      }
    }

    // Fallback: General search for product label/producer
    const fallbackQueries = [
      `"${productName}" "prodotto da" OR "fabbricato da" OR "produttore"`,
      `"${productName}" etichetta ingredienti produttore`,
      `"${productName}" scheda prodotto produttore azienda`
    ];

    for (const query of fallbackQueries) {
      console.log('🔍 Fallback search:', query);
      const results = await searchService.searchBrandInfo(query);

      producerName = this.extractProducerFromSnippets(results.snippets);
      if (producerName) {
        console.log('✅ Found producer from fallback search:', producerName);
        return producerName;
      }
    }

    // Last resort: use brand name from logo/web entity
    if (visionResults.logos.length > 0) {
      return visionResults.logos[0].description;
    }
    if (visionResults.webEntities.length > 0) {
      return visionResults.webEntities[0].description;
    }

    return null;
  }

  /**
   * Extract producer name from search result snippets
   */
  extractProducerFromSnippets(snippets) {
    if (!snippets || snippets.length === 0) return null;

    const allText = snippets.join(' ');

    // Patterns to find producer in snippets (ordered by reliability)
    const patterns = [
      // "Prodotto da: Company Name" or "Prodotto da Company S.p.A."
      /prodotto\s+(?:e\s+confezionato\s+)?da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú0-9\s&.',-]+?(?:S\.?p\.?A\.?|S\.?r\.?l\.?|S\.?a\.?s\.?|S\.?n\.?c\.?|Inc\.?|Ltd\.?|GmbH|S\.?A\.?))/gi,
      /prodotto\s+(?:e\s+confezionato\s+)?da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú\s&.',-]{3,40})(?:\s*[-–,.]|\s+via\s+|\s+V\.\s+|\s+sede|\s+stabil)/gi,

      // "Fabbricato da: Company"
      /fabbricato\s+da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú0-9\s&.',-]+?(?:S\.?p\.?A\.?|S\.?r\.?l\.?|S\.?a\.?s\.?|S\.?n\.?c\.?))/gi,
      /fabbricato\s+da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú\s&.',-]{3,40})(?:\s*[-–,.]|\s+via\s+|\s+V\.\s+)/gi,

      // "Produttore: Company"
      /produttore[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú0-9\s&.',-]+?(?:S\.?p\.?A\.?|S\.?r\.?l\.?|S\.?a\.?s\.?|S\.?n\.?c\.?))/gi,
      /produttore[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú\s&.',-]{3,40})(?:\s*[-–,.]|\s+via\s+|\s+V\.\s+)/gi,

      // "Confezionato da"
      /confezionato\s+da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú0-9\s&.',-]+?(?:S\.?p\.?A\.?|S\.?r\.?l\.?))/gi,

      // "Distribuito da" (for imported products)
      /distribuit[oa]\s+da[:\s]+([A-ZÀ-Ú][A-Za-zÀ-ú0-9\s&.',-]+?(?:S\.?p\.?A\.?|S\.?r\.?l\.?))/gi,

      // Company with legal suffix standalone
      /([A-ZÀ-Ú][A-Za-zÀ-ú\s&.']+\s+(?:S\.p\.A\.|S\.r\.l\.|S\.a\.s\.|S\.n\.c\.))/g
    ];

    for (const pattern of patterns) {
      const matches = allText.matchAll(pattern);
      for (const match of matches) {
        if (match[1]) {
          let producer = match[1].trim();
          // Clean up
          producer = producer.replace(/\s+/g, ' ').replace(/[,.]$/, '');
          // Validate length
          if (producer.length >= 3 && producer.length <= 60) {
            return producer;
          }
        }
      }
    }

    return null;
  }

  /**
   * STEP 3: Search for company/producer details
   */
  async findCompanyDetails(producerName, productName) {
    const info = {
      groupName: null,
      headquarterCountry: null,
      headquarterCity: null,
      sourceUrls: []
    };

    if (!producerName) {
      // Try with product/brand name
      if (!productName) return info;
      producerName = productName;
    }

    // Search for company details
    const companyQueries = [
      `"${producerName}" azienda sede legale Italia`,
      `"${producerName}" wikipedia azienda`,
      `"${producerName}" gruppo società controllante`
    ];

    for (const query of companyQueries) {
      console.log('🔍 Company search:', query);
      const results = await searchService.searchBrandInfo(query);

      // Extract info from results
      this.enrichCompanyInfo(info, results);

      // Add URLs as sources
      if (results.urls) {
        info.sourceUrls = [...info.sourceUrls, ...results.urls];
      }

      // If we found country, we have enough
      if (info.headquarterCountry) break;
    }

    // Try Knowledge Graph
    const kgResults = await searchService.searchKnowledgeGraph(producerName);
    if (kgResults.entities && kgResults.entities.length > 0) {
      const org = kgResults.entities.find(e =>
        e.type?.includes('Organization') || e.type?.includes('Corporation')
      );
      if (org) {
        if (!info.groupName && org.name !== producerName) {
          info.groupName = org.name;
        }
        if (org.url) {
          info.sourceUrls.unshift(org.url);
        }
      }
    }

    // Dedupe URLs
    info.sourceUrls = [...new Set(info.sourceUrls)].slice(0, 5);

    return info;
  }

  /**
   * Extract company info from search results
   */
  enrichCompanyInfo(info, searchResults) {
    const allText = (searchResults.snippets || []).join(' ').toLowerCase();
    const allTitles = (searchResults.items || []).map(i => i.title || '').join(' ').toLowerCase();
    const combined = allText + ' ' + allTitles;

    // Country patterns
    const countryPatterns = [
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+|a\s+)?italia|italiana|italy|azienda\s+italiana/i, country: 'Italia' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?svizzera|swiss|switzerland/i, country: 'Svizzera' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?francia|french|france/i, country: 'Francia' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?germania|german|germany/i, country: 'Germania' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?stati\s+uniti|usa|american/i, country: 'Stati Uniti' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?spagna|spanish|spain/i, country: 'Spagna' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?regno\s+unito|uk|british/i, country: 'Regno Unito' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?paesi\s+bassi|olanda|dutch|netherlands/i, country: 'Paesi Bassi' },
      { pattern: /sede\s+(?:legale\s+)?(?:in\s+)?belgio|belgium|belgian/i, country: 'Belgio' },
      { pattern: /multinazionale\s+italiana|fondata\s+in\s+italia/i, country: 'Italia' }
    ];

    if (!info.headquarterCountry) {
      for (const { pattern, country } of countryPatterns) {
        if (pattern.test(combined)) {
          info.headquarterCountry = country;
          break;
        }
      }
    }

    // Group/parent company patterns
    if (!info.groupName) {
      const groupPatterns = [
        /(?:gruppo|group|parte\s+di|appartiene\s+a|controllata\s+da|owned\s+by)\s+([A-ZÀ-Ú][A-Za-zÀ-ú\s&.]+)/gi,
        /([A-ZÀ-Ú][A-Za-zÀ-ú\s&.]+)\s+(?:group|gruppo)/gi
      ];

      for (const pattern of groupPatterns) {
        const match = combined.match(pattern);
        if (match) {
          info.groupName = match[1] || match[0];
          info.groupName = info.groupName.replace(/gruppo|group/gi, '').trim();
          if (info.groupName.length > 2) break;
        }
      }
    }

    // Italian cities (for headquarters)
    if (!info.headquarterCity) {
      const cityPatterns = [
        /sede\s+(?:legale\s+)?(?:a|in)\s+([A-ZÀ-Ú][a-zà-ú]+)/i,
        /(?:milano|roma|torino|bologna|firenze|napoli|parma|alba|perugia)/i
      ];

      for (const pattern of cityPatterns) {
        const match = combined.match(pattern);
        if (match) {
          info.headquarterCity = match[1] || match[0];
          info.headquarterCity = info.headquarterCity.charAt(0).toUpperCase() + info.headquarterCity.slice(1).toLowerCase();
          break;
        }
      }
    }
  }

  /**
   * STEP 4: Build final result object
   */
  buildFinalResult(visionResults, productName, producerName, companyInfo) {
    // Extract brand from vision
    let brandName = null;
    if (visionResults.logos.length > 0) {
      brandName = visionResults.logos[0].description;
    } else if (visionResults.webEntities.length > 0) {
      brandName = visionResults.webEntities[0].description;
    }

    // Calculate confidence
    let confidence = 0;
    if (productName) confidence += 0.2;
    if (brandName) confidence += 0.2;
    if (producerName) confidence += 0.25;
    if (companyInfo.headquarterCountry) confidence += 0.15;
    if (companyInfo.groupName) confidence += 0.1;
    if (companyInfo.sourceUrls.length > 0) confidence += 0.1;

    return {
      productId: null,
      productName: productName,
      brandName: brandName,
      producerName: producerName,
      groupName: companyInfo.groupName,
      headquarterCountry: companyInfo.headquarterCountry,
      headquarterCity: companyInfo.headquarterCity,
      sourceUrls: companyInfo.sourceUrls,
      confidence: Math.min(confidence, 1),
      rawData: {
        labels: visionResults.labels.slice(0, 5),
        detectedText: visionResults.detectedText?.substring(0, 500) || ''
      }
    };
  }
}

module.exports = new ProductEnricher();

