export function sanitizeHTML(html) {
    if (typeof html !== 'string')
        return '';
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;
    const allowedTags = ['p', 'br', 'strong', 'em', 'b', 'i', 'u', 'a', 'img', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'blockquote', 'pre', 'code'];
    const allowedAttributes = ['href', 'src', 'alt', 'title', 'target', 'class'];
    const allowedSchemes = ['http:', 'https:', 'mailto:'];
    const cleanNode = (node) => {
        if (node.nodeType === 1) {
            const element = node;
            const tagName = element.tagName.toLowerCase();
            if (!allowedTags.includes(tagName)) {
                const text = document.createTextNode(element.textContent || '');
                element.parentNode?.replaceChild(text, element);
                return;
            }
            const attrs = Array.from(element.attributes);
            attrs.forEach((attr) => {
                if (!allowedAttributes.includes(attr.name.toLowerCase())) {
                    element.removeAttribute(attr.name);
                    return;
                }
                if ((attr.name === 'href' || attr.name === 'src') && attr.value) {
                    try {
                        const url = new URL(attr.value);
                        if (!allowedSchemes.includes(url.protocol)) {
                            element.removeAttribute(attr.name);
                        }
                    }
                    catch {
                        if (!attr.value.startsWith('#') && !attr.value.startsWith('/')) {
                            element.removeAttribute(attr.name);
                        }
                    }
                }
            });
            if (tagName === 'a') {
                element.setAttribute('rel', 'noopener noreferrer');
                if (!element.getAttribute('target')) {
                    element.setAttribute('target', '_blank');
                }
            }
            Array.from(element.childNodes).forEach(cleanNode);
        }
    };
    Array.from(tempDiv.childNodes).forEach(cleanNode);
    return tempDiv.innerHTML;
}
export function encodeHTML(str) {
    if (typeof str !== 'string')
        return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
export function safeLocalStorage() {
    return {
        setItem(key, value) {
            try {
                if (typeof value === 'string') {
                    localStorage.setItem(key, value);
                }
            }
            catch (e) {
                console.warn('localStorage setItem failed', e);
            }
        },
        getItem(key) {
            try {
                return localStorage.getItem(key);
            }
            catch (e) {
                console.warn('localStorage getItem failed', e);
                return null;
            }
        },
        removeItem(key) {
            try {
                localStorage.removeItem(key);
            }
            catch (e) {
                console.warn('localStorage removeItem failed', e);
            }
        },
        setJSON(key, value) {
            try {
                const sanitizedValue = JSON.stringify(value);
                this.setItem(key, sanitizedValue);
            }
            catch (e) {
                console.warn('localStorage setJSON failed', e);
            }
        },
        getJSON(key, defaultValue) {
            try {
                const item = this.getItem(key);
                if (item === null)
                    return defaultValue;
                return JSON.parse(item);
            }
            catch (e) {
                console.warn('localStorage getJSON failed', e);
                return defaultValue;
            }
        }
    };
}
