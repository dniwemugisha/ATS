import axiosInstance from '../utils/axios';

const API_URL = '/files';

export const fileUploadService = {
  // Upload resume
  uploadResume: async (file: File) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await axiosInstance.post(`${API_URL}/upload/resume`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      return response.data.url;
    } catch (error) {
      throw error;
    }
  },
  
  // Upload cover letter
  uploadCoverLetter: async (file: File) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await axiosInstance.post(`${API_URL}/upload/cover-letter`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      return response.data.url;
    } catch (error) {
      throw error;
    }
  }
};

export default fileUploadService;
