/**
 * Google Custom Search API Service
 * Searches for company/brand information
 */

const axios = require('axios');

class SearchService {
  constructor() {
    this.apiKey = process.env.GOOGLE_CUSTOM_SEARCH_API_KEY;
    this.searchEngineId = process.env.GOOGLE_CUSTOM_SEARCH_ENGINE_ID;
    this.baseUrl = 'https://www.googleapis.com/customsearch/v1';
  }

  /**
   * Search for brand/company information
   * @param {string} query - Search query (brand + product name)
   * @returns {Promise<Object>} Search results with extracted info
   */
  async searchBrandInfo(query) {
    try {
      // Use simple brand query for better results
      const enhancedQuery = `${query} azienda produttore proprietario`;

      console.log('🔎 Custom Search Query:', enhancedQuery);
      console.log('🔑 Using API Key:', this.apiKey?.substring(0, 10) + '...');
      console.log('🆔 Using CSE ID:', this.searchEngineId);

      const response = await axios.get(this.baseUrl, {
        params: {
          key: this.apiKey,
          cx: this.searchEngineId,
          q: enhancedQuery,
          num: 5, // Get top 5 results
          lr: 'lang_it|lang_en', // Italian and English results
          safe: 'active'
        }
      });

      return this.parseSearchResults(response.data);

    } catch (error) {
      console.error('Custom Search API error:', error.response?.data || error.message);
      // Return empty results on error, don't fail the whole analysis
      return {
        items: [],
        snippets: [],
        urls: []
      };
    }
  }

  /**
   * Parse search results
   */
  parseSearchResults(data) {
    const parsed = {
      items: [],
      snippets: [],
      urls: []
    };

    if (data.items && data.items.length > 0) {
      parsed.items = data.items.map(item => ({
        title: item.title,
        link: item.link,
        snippet: item.snippet,
        displayLink: item.displayLink
      }));

      parsed.snippets = data.items
        .filter(item => item.snippet)
        .map(item => item.snippet);
      
      parsed.urls = data.items.map(item => item.link);
    }

    return parsed;
  }

  /**
   * Try to search using Knowledge Graph API as fallback
   * @param {string} query - Search query
   */
  async searchKnowledgeGraph(query) {
    try {
      const response = await axios.get('https://kgsearch.googleapis.com/v1/entities:search', {
        params: {
          key: this.apiKey,
          query: query,
          types: 'Organization,Corporation,Brand',
          limit: 5,
          languages: 'it,en'
        }
      });

      return this.parseKnowledgeGraphResults(response.data);

    } catch (error) {
      console.error('Knowledge Graph API error:', error.message);
      return { entities: [] };
    }
  }

  /**
   * Parse Knowledge Graph results
   */
  parseKnowledgeGraphResults(data) {
    const entities = [];
    
    if (data.itemListElement) {
      data.itemListElement.forEach(item => {
        const entity = item.result;
        if (entity) {
          entities.push({
            name: entity.name,
            type: entity['@type'],
            description: entity.description,
            detailedDescription: entity.detailedDescription?.articleBody,
            url: entity.url || entity.detailedDescription?.url
          });
        }
      });
    }

    return { entities };
  }
}

module.exports = new SearchService();

