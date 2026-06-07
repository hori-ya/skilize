import apiClient from '../../../shared/api/client';

export const downloadInventoryReport = (id: number) =>
  apiClient.get(`/inventories/${id}/report`, { responseType: 'blob' });
