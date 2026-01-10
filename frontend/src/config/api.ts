/**
 * Backend API server base URL configuration
 * Points to the Spring Boot backend server
 */
export const BACKEND_API_BASE_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

/**
 * Backend API endpoints
 */
export const BACKEND_API_ENDPOINTS = {
  jobs: `${BACKEND_API_BASE_URL}/api`,
  job: (jobId: string) => `${BACKEND_API_BASE_URL}/api/${jobId}`,
  jobScore: (jobId: string) => `${BACKEND_API_BASE_URL}/api/score/${jobId}`,
  jobIndictments: (jobId: string) => `${BACKEND_API_BASE_URL}/api/indictments/${jobId}`,
  submitSchedule: `${BACKEND_API_BASE_URL}/api`,
} as const;
