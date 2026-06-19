import axios from 'axios';

const api = axios.create({
  baseURL: '/api'
});

export const getProducts = () => api.get('/products');

export const getProduct = (id) => api.get(`/products/${id}`);

export const createProduct = (data) => api.post('/products', data);

export const updateProduct = (id, data) => api.put(`/products/${id}`, data);

export const deleteProduct = (id) => api.delete(`/products/${id}`);

export const searchProducts = (query) => api.get(`/products/search?q=${encodeURIComponent(query)}`);

export const importCsv = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/products/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

export const downloadErrors = (errorFileId) =>
  api.get(`/products/import/errors/${errorFileId}`, { responseType: 'blob' });
