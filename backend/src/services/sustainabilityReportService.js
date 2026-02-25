/**
 * Sustainability Report Service
 * Aggregates packaging data from all receipt products into a report
 */

class SustainabilityReportService {
  /**
   * Generate a sustainability report from receipt + packaging data
   * @param {Object} receiptData - Parsed receipt data from receiptService
   * @param {Array} packagingData - Array of PackagingInfo from packagingResearchService
   * @returns {SustainabilityReport}
   */
  generateReport(receiptData, packagingData) {
    const products = receiptData.products || [];

    // Merge product info with packaging info
    const enrichedProducts = products.map((product) => {
      const packaging = packagingData.find(
        (p) =>
          p.productName.toLowerCase().includes(product.name.toLowerCase()) ||
          product.name.toLowerCase().includes(p.productName.toLowerCase())
      ) || null;

      return {
        ...product,
        packaging: packaging || null,
      };
    });

    // Aggregate totals
    const totals = this._calculateTotals(enrichedProducts);

    // Rank products by pollution impact
    const pollutionRanking = this._rankByPollution(enrichedProducts);

    // Generate eco tips
    const ecoTips = this._generateEcoTips(enrichedProducts, totals);

    // Material breakdown (pie chart data)
    const materialBreakdown = this._buildMaterialBreakdown(totals);

    // Per-product recyclability scores
    const recyclabilityData = enrichedProducts
      .filter((p) => p.packaging)
      .map((p) => ({
        productName: p.name,
        brand: p.brand || null,
        recyclabilityScore: p.packaging.recyclabilityScore,
        recyclabilityLabel: p.packaging.recyclabilityLabel,
        primaryMaterial: p.packaging.primaryPackagingMaterial,
      }))
      .sort((a, b) => a.recyclabilityScore - b.recyclabilityScore);

    return {
      // Receipt metadata
      storeName: receiptData.storeName || null,
      storeAddress: receiptData.storeAddress || null,
      receiptDate: receiptData.receiptDate || null,
      receiptNumber: receiptData.receiptNumber || null,
      totalSpent: receiptData.totalAmount || null,
      currency: receiptData.currency || 'EUR',
      totalProducts: products.length,
      analyzedProducts: enrichedProducts.filter((p) => p.packaging).length,

      // Plastic data
      totalPlasticGrams: totals.totalPlasticGrams,
      totalRecyclablePlasticGrams: totals.totalRecyclablePlasticGrams,
      totalNonRecyclablePlasticGrams: totals.totalNonRecyclablePlasticGrams,
      plasticRecyclabilityPercentage:
        totals.totalPlasticGrams > 0
          ? Math.round((totals.totalRecyclablePlasticGrams / totals.totalPlasticGrams) * 100)
          : 0,

      // Other materials
      totalCardboardGrams: totals.totalCardboardGrams,
      totalGlassGrams: totals.totalGlassGrams,
      totalAluminumGrams: totals.totalAluminumGrams,
      totalOtherGrams: totals.totalOtherGrams,
      totalPackagingGrams: totals.totalPackagingGrams,

      // CO2 estimate
      totalCo2PackagingKg: totals.totalCo2Kg,

      // Scores
      averageRecyclabilityScore: totals.averageRecyclabilityScore,
      overallEcoGrade: this._calculateEcoGrade(totals.averageRecyclabilityScore),

      // Rankings
      pollutionRanking,
      recyclabilityData,

      // Charts
      materialBreakdown,

      // Tips
      ecoTips,

      // Full enriched product list
      products: enrichedProducts,

      // Report metadata
      generatedAt: new Date().toISOString(),
      reportVersion: '1.0',
    };
  }

  _calculateTotals(enrichedProducts) {
    let totalPlasticGrams = 0;
    let totalRecyclablePlasticGrams = 0;
    let totalNonRecyclablePlasticGrams = 0;
    let totalCardboardGrams = 0;
    let totalGlassGrams = 0;
    let totalAluminumGrams = 0;
    let totalOtherGrams = 0;
    let totalPackagingGrams = 0;
    let totalCo2Kg = 0;
    let recyclabilityScores = [];

    for (const p of enrichedProducts) {
      if (!p.packaging) continue;
      const qty = p.quantity || 1;
      const pkg = p.packaging;

      totalPlasticGrams += (pkg.plasticGrams || 0) * qty;
      totalRecyclablePlasticGrams += (pkg.recyclablePlasticGrams || 0) * qty;
      totalNonRecyclablePlasticGrams += (pkg.nonRecyclablePlasticGrams || 0) * qty;
      totalCardboardGrams += (pkg.cardboardGrams || 0) * qty;
      totalGlassGrams += (pkg.glassGrams || 0) * qty;
      totalAluminumGrams += (pkg.aluminumGrams || 0) * qty;
      totalOtherGrams += (pkg.otherMaterialGrams || 0) * qty;
      totalPackagingGrams += (pkg.totalPackagingWeightGrams || 0) * qty;
      totalCo2Kg += (pkg.co2PackagingKg || 0) * qty;

      if (pkg.recyclabilityScore > 0) {
        recyclabilityScores.push(pkg.recyclabilityScore);
      }
    }

    const averageRecyclabilityScore =
      recyclabilityScores.length > 0
        ? Math.round(
            recyclabilityScores.reduce((a, b) => a + b, 0) / recyclabilityScores.length
          )
        : 0;

    return {
      totalPlasticGrams: Math.round(totalPlasticGrams),
      totalRecyclablePlasticGrams: Math.round(totalRecyclablePlasticGrams),
      totalNonRecyclablePlasticGrams: Math.round(totalNonRecyclablePlasticGrams),
      totalCardboardGrams: Math.round(totalCardboardGrams),
      totalGlassGrams: Math.round(totalGlassGrams),
      totalAluminumGrams: Math.round(totalAluminumGrams),
      totalOtherGrams: Math.round(totalOtherGrams),
      totalPackagingGrams: Math.round(totalPackagingGrams),
      totalCo2Kg: Math.round(totalCo2Kg * 100) / 100,
      averageRecyclabilityScore,
    };
  }

  _rankByPollution(enrichedProducts) {
    return enrichedProducts
      .filter((p) => p.packaging && p.packaging.dataConfidence > 0)
      .map((p) => {
        const qty = p.quantity || 1;
        const pkg = p.packaging;
        const pollutionScore =
          (pkg.nonRecyclablePlasticGrams || 0) * qty * 3 + // non-recyclable plastic is worst
          (pkg.plasticGrams || 0) * qty * 1.5 +           // plastic in general
          (pkg.co2PackagingKg || 0) * qty * 100;          // CO2 contribution

        return {
          productName: p.name,
          brand: p.brand || null,
          totalPrice: p.totalPrice || null,
          primaryMaterial: pkg.primaryPackagingMaterial,
          nonRecyclablePlasticGrams: Math.round((pkg.nonRecyclablePlasticGrams || 0) * qty),
          totalPlasticGrams: Math.round((pkg.plasticGrams || 0) * qty),
          co2PackagingKg: Math.round((pkg.co2PackagingKg || 0) * qty * 100) / 100,
          recyclabilityScore: pkg.recyclabilityScore,
          pollutionScore: Math.round(pollutionScore),
          environmentalNotes: pkg.environmentalNotes || null,
          ecoAlternativeSuggestion: this._suggestAlternative(p),
        };
      })
      .sort((a, b) => b.pollutionScore - a.pollutionScore);
  }

  _suggestAlternative(product) {
    const materialMap = {
      plastica: 'Cerca alternative in vetro o cartone',
      'non riciclabile': 'Scegli prodotti con packaging certificato riciclabile',
      tetrapack: 'Verifica se disponibile in versione vetro o sfusa',
      polistirolo: 'Evita prodotti con polistirolo, preferisci carta',
    };

    const pkg = product.packaging;
    if (!pkg) return null;

    for (const [keyword, suggestion] of Object.entries(materialMap)) {
      if (
        (pkg.primaryPackagingMaterial || '').toLowerCase().includes(keyword) ||
        (pkg.environmentalNotes || '').toLowerCase().includes(keyword)
      ) {
        return suggestion;
      }
    }

    if (pkg.recyclabilityScore < 30) {
      return 'Cerca alternative con packaging più sostenibile o acquista sfuso';
    }

    return null;
  }

  _buildMaterialBreakdown(totals) {
    const items = [
      { material: 'Plastica riciclabile', grams: totals.totalRecyclablePlasticGrams, color: '#4CAF50' },
      { material: 'Plastica non riciclabile', grams: totals.totalNonRecyclablePlasticGrams, color: '#F44336' },
      { material: 'Cartone/Carta', grams: totals.totalCardboardGrams, color: '#FF9800' },
      { material: 'Vetro', grams: totals.totalGlassGrams, color: '#2196F3' },
      { material: 'Alluminio', grams: totals.totalAluminumGrams, color: '#9E9E9E' },
      { material: 'Altro', grams: totals.totalOtherGrams, color: '#795548' },
    ].filter((item) => item.grams > 0);

    const total = items.reduce((sum, item) => sum + item.grams, 0);

    return items.map((item) => ({
      ...item,
      percentage: total > 0 ? Math.round((item.grams / total) * 100) : 0,
    }));
  }

  _calculateEcoGrade(averageScore) {
    if (averageScore >= 80) return { grade: 'A', label: 'Eccellente', color: '#4CAF50' };
    if (averageScore >= 60) return { grade: 'B', label: 'Buono', color: '#8BC34A' };
    if (averageScore >= 40) return { grade: 'C', label: 'Sufficiente', color: '#FF9800' };
    if (averageScore >= 20) return { grade: 'D', label: 'Scarso', color: '#FF5722' };
    return { grade: 'E', label: 'Pessimo', color: '#F44336' };
  }

  _generateEcoTips(enrichedProducts, totals) {
    const tips = [];

    if (totals.totalNonRecyclablePlasticGrams > 100) {
      tips.push({
        icon: '♻️',
        title: 'Riduci la plastica non riciclabile',
        description: `Questa spesa contiene ${totals.totalNonRecyclablePlasticGrams}g di plastica non riciclabile. Cerca alternative in vetro o usa contenitori riutilizzabili.`,
        priority: 'alta',
      });
    }

    if (totals.totalPlasticGrams > 200) {
      tips.push({
        icon: '🛒',
        title: 'Preferisci prodotti sfusi',
        description: 'Acquistare prodotti sfusi riduce significativamente la quantità di plastica nella spesa.',
        priority: 'media',
      });
    }

    if (totals.averageRecyclabilityScore < 50) {
      tips.push({
        icon: '🌱',
        title: 'Scegli prodotti con certificazioni ambientali',
        description: 'Cerca prodotti con certificazioni FSC, RecyClass o EU Ecolabel per garantire packaging sostenibile.',
        priority: 'media',
      });
    }

    if (totals.totalCo2Kg > 0.5) {
      tips.push({
        icon: '🌍',
        title: 'Impatto CO2 del packaging',
        description: `Il packaging di questa spesa ha generato circa ${totals.totalCo2Kg}kg di CO2. Scegli prodotti locali e con packaging leggero.`,
        priority: 'bassa',
      });
    }

    // Category-specific tips
    const hasBottledWater = enrichedProducts.some((p) =>
      p.name.toLowerCase().includes('acqua') && p.packaging?.primaryPackagingMaterial === 'plastica'
    );
    if (hasBottledWater) {
      tips.push({
        icon: '💧',
        title: 'Usa una bottiglia riutilizzabile',
        description: "L'acqua del rubinetto italiana è sicura e potabile. Una bottiglia riutilizzabile risparmia centinaia di bottiglie all'anno.",
        priority: 'alta',
      });
    }

    return tips;
  }
}

module.exports = new SustainabilityReportService();
