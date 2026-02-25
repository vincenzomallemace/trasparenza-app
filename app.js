// Supermarket Compare App
class SupermarketCompare {
    constructor() {
        this.products = this.loadProducts();
        this.initializeElements();
        this.attachEventListeners();
        this.render();
    }

    initializeElements() {
        this.form = document.getElementById('productForm');
        this.productsBody = document.getElementById('productsBody');
        this.emptyState = document.getElementById('emptyState');
        this.productsTable = document.getElementById('productsTable');
        this.statsSection = document.getElementById('statsSection');
        this.clearAllBtn = document.getElementById('clearAll');
        
        // Form inputs
        this.productNameInput = document.getElementById('productName');
        this.supermarketInput = document.getElementById('supermarket');
        this.priceInput = document.getElementById('price');
        this.quantityInput = document.getElementById('quantity');
        this.unitInput = document.getElementById('unit');
        
        // Stats elements
        this.totalProductsEl = document.getElementById('totalProducts');
        this.bestDealEl = document.getElementById('bestDeal');
        this.avgPriceEl = document.getElementById('avgPrice');
    }

    attachEventListeners() {
        this.form.addEventListener('submit', (e) => this.handleAddProduct(e));
        this.clearAllBtn.addEventListener('click', () => this.handleClearAll());
    }

    loadProducts() {
        const stored = localStorage.getItem('supermarketProducts');
        return stored ? JSON.parse(stored) : [];
    }

    saveProducts() {
        localStorage.setItem('supermarketProducts', JSON.stringify(this.products));
    }

    handleAddProduct(e) {
        e.preventDefault();
        
        const product = {
            id: Date.now(),
            name: this.productNameInput.value.trim(),
            supermarket: this.supermarketInput.value.trim(),
            price: parseFloat(this.priceInput.value),
            quantity: parseFloat(this.quantityInput.value),
            unit: this.unitInput.value
        };

        // Calculate price per unit (normalize to base unit)
        product.pricePerUnit = this.calculatePricePerUnit(product);
        
        this.products.push(product);
        this.saveProducts();
        this.render();
        this.form.reset();
        
        // Show success feedback
        this.showNotification('Prodotto aggiunto con successo!');
    }

    calculatePricePerUnit(product) {
        let normalizedQuantity = product.quantity;
        
        // Normalize to base units (g, ml, pz)
        switch(product.unit) {
            case 'kg':
                normalizedQuantity = product.quantity * 1000;
                break;
            case 'l':
                normalizedQuantity = product.quantity * 1000;
                break;
        }
        
        return product.price / normalizedQuantity;
    }

    getBaseUnit(unit) {
        switch(unit) {
            case 'kg':
            case 'g':
                return 'g';
            case 'l':
            case 'ml':
                return 'ml';
            default:
                return unit;
        }
    }

    formatPricePerUnit(product) {
        const baseUnit = this.getBaseUnit(product.unit);
        let displayQuantity = 100; // Default display per 100g/ml
        
        if (baseUnit === 'pz') {
            displayQuantity = 1;
        }
        
        const pricePerDisplay = product.pricePerUnit * displayQuantity;
        
        if (baseUnit === 'pz') {
            return `€${pricePerDisplay.toFixed(2)}/pz`;
        } else {
            return `€${pricePerDisplay.toFixed(2)}/100${baseUnit}`;
        }
    }

    handleDeleteProduct(id) {
        if (confirm('Sei sicuro di voler eliminare questo prodotto?')) {
            this.products = this.products.filter(p => p.id !== id);
            this.saveProducts();
            this.render();
            this.showNotification('Prodotto eliminato');
        }
    }

    handleClearAll() {
        if (this.products.length === 0) return;
        
        if (confirm('Sei sicuro di voler eliminare tutti i prodotti?')) {
            this.products = [];
            this.saveProducts();
            this.render();
            this.showNotification('Tutti i prodotti sono stati eliminati');
        }
    }

    getConvenienceRating(product, allProducts) {
        // Group products by name and base unit
        const baseUnit = this.getBaseUnit(product.unit);
        const similarProducts = allProducts.filter(p => 
            p.name.toLowerCase() === product.name.toLowerCase() &&
            this.getBaseUnit(p.unit) === baseUnit
        );

        if (similarProducts.length < 2) {
            return { rating: 'average', label: 'Unico' };
        }

        // Find min and max prices per unit
        const prices = similarProducts.map(p => p.pricePerUnit);
        const minPrice = Math.min(...prices);
        const maxPrice = Math.max(...prices);
        const range = maxPrice - minPrice;

        // Calculate position in range
        const position = (product.pricePerUnit - minPrice) / range;

        if (product.pricePerUnit === minPrice) {
            return { rating: 'best', label: '🏆 Migliore' };
        } else if (position <= 0.33) {
            return { rating: 'good', label: '✓ Buono' };
        } else {
            return { rating: 'average', label: '○ Medio' };
        }
    }

    render() {
        if (this.products.length === 0) {
            this.emptyState.style.display = 'block';
            this.productsTable.style.display = 'none';
            this.statsSection.style.display = 'none';
            return;
        }

        this.emptyState.style.display = 'none';
        this.productsTable.style.display = 'block';
        this.statsSection.style.display = 'block';

        this.renderProducts();
        this.renderStats();
    }

    renderProducts() {
        this.productsBody.innerHTML = '';

        this.products.forEach(product => {
            const convenience = this.getConvenienceRating(product, this.products);
            const row = document.createElement('tr');
            
            row.innerHTML = `
                <td><strong>${this.escapeHtml(product.name)}</strong></td>
                <td>${this.escapeHtml(product.supermarket)}</td>
                <td class="price-highlight">€${product.price.toFixed(2)}</td>
                <td>${product.quantity} ${product.unit}</td>
                <td>${this.formatPricePerUnit(product)}</td>
                <td><span class="badge badge-${convenience.rating}">${convenience.label}</span></td>
                <td>
                    <button class="btn-danger" onclick="app.handleDeleteProduct(${product.id})">
                        🗑️ Elimina
                    </button>
                </td>
            `;
            
            this.productsBody.appendChild(row);
        });
    }

    renderStats() {
        // Total products
        this.totalProductsEl.textContent = this.products.length;

        // Best deal
        if (this.products.length > 0) {
            const bestProduct = this.products.reduce((best, current) => {
                const bestConvenience = this.getConvenienceRating(best, this.products);
                const currentConvenience = this.getConvenienceRating(current, this.products);
                
                if (currentConvenience.rating === 'best' && bestConvenience.rating !== 'best') {
                    return current;
                }
                if (currentConvenience.rating === 'best' && bestConvenience.rating === 'best') {
                    return current.pricePerUnit < best.pricePerUnit ? current : best;
                }
                return best;
            });
            
            this.bestDealEl.textContent = `${bestProduct.name} @ ${bestProduct.supermarket}`;
        } else {
            this.bestDealEl.textContent = '-';
        }

        // Average price
        if (this.products.length > 0) {
            const avgPrice = this.products.reduce((sum, p) => sum + p.price, 0) / this.products.length;
            this.avgPriceEl.textContent = `€${avgPrice.toFixed(2)}`;
        } else {
            this.avgPriceEl.textContent = '€0.00';
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showNotification(message) {
        // Simple notification - could be enhanced with a toast library
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #10b981;
            color: white;
            padding: 16px 24px;
            border-radius: 8px;
            box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
            z-index: 1000;
            animation: slideIn 0.3s ease;
        `;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }
}

// Add animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Initialize app
const app = new SupermarketCompare();

// Made with Bob
