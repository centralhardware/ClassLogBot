// Template utility functions
// Works with <template> elements defined in index.html

const TemplateUtils = {
    /**
     * Clone template and fill it with data
     * @param {string} templateId - ID of the template element
     * @param {Object} data - Data to fill template with
     * @param {Function} fillFn - Optional custom fill function
     * @returns {HTMLElement} - Filled template element
     */
    create(templateId, data, fillFn) {
        const template = document.getElementById(templateId);
        if (!template) {
            console.error(`Template not found: ${templateId}`);
            return null;
        }

        const clone = template.content.cloneNode(true);
        const element = clone.firstElementChild;

        if (fillFn) {
            fillFn(element, data);
        }

        return element;
    },

    /**
     * Create multiple elements from template
     * @param {string} templateId - ID of the template element
     * @param {Array} dataArray - Array of data objects
     * @param {Function} fillFn - Custom fill function (element, data, index)
     * @returns {DocumentFragment} - Fragment containing all elements
     */
    createMany(templateId, dataArray, fillFn) {
        const fragment = document.createDocumentFragment();
        dataArray.forEach((data, index) => {
            const template = document.getElementById(templateId);
            if (!template) {
                console.error(`Template not found: ${templateId}`);
                return;
            }

            const clone = template.content.cloneNode(true);
            const element = clone.firstElementChild;

            if (fillFn) {
                fillFn(element, data, index);
            }

            fragment.appendChild(element);
        });
        return fragment;
    },

    /**
     * Helper to set text content of element by selector
     */
    setText(element, selector, text) {
        const target = element.querySelector(selector);
        if (target) {
            target.textContent = text || '';
        }
    },

    /**
     * Helper to set HTML content of element by selector
     */
    setHTML(element, selector, html) {
        const target = element.querySelector(selector);
        if (target) {
            target.innerHTML = html || '';
        }
    },

    /**
     * Helper to set attribute of element by selector
     */
    setAttribute(element, selector, attr, value) {
        const target = selector ? element.querySelector(selector) : element;
        if (target) {
            target.setAttribute(attr, value);
        }
    },

    /**
     * Helper to set data attribute
     */
    setData(element, selector, key, value) {
        const target = selector ? element.querySelector(selector) : element;
        if (target) {
            target.dataset[key] = value;
        }
    },

    /**
     * Helper to add event listener
     */
    on(element, selector, event, handler) {
        const target = selector ? element.querySelector(selector) : element;
        if (target) {
            target.addEventListener(event, handler);
        }
    },

    /**
     * Helper to show/hide element
     */
    toggle(element, selector, show) {
        const target = selector ? element.querySelector(selector) : element;
        if (target) {
            target.style.display = show ? '' : 'none';
        }
    },

    /**
     * Helper to add/remove class
     */
    toggleClass(element, selector, className, add) {
        const target = selector ? element.querySelector(selector) : element;
        if (target) {
            if (add) {
                target.classList.add(className);
            } else {
                target.classList.remove(className);
            }
        }
    }
};
