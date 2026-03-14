/**
 * Job model matching the backend JobResponse DTO
 */
export interface Job {
  id: number;
  name: string;
  description: string | null;
  cronExpression: string | null;
  enabled: boolean;
  status: JobStatus;
  maxRetries: number;
  retryCount: number;
  timeoutSeconds: number;
  nextRunTime: string | null;
  lastRunTime: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Job status enum matching backend JobStatus
 */
export enum JobStatus {
  PENDING = 'PENDING',
  SCHEDULED = 'SCHEDULED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  RETRYING = 'RETRYING',
  PAUSED = 'PAUSED',
  CANCELLED = 'CANCELLED'
}

/**
 * Create job request matching backend CreateJobRequest
 */
export interface CreateJobRequest {
  name: string;
  description?: string;
  cronExpression?: string;
  enabled?: boolean;
  maxRetries?: number;
  timeoutSeconds?: number;
}

/**
 * Update job request matching backend UpdateJobRequest
 */
export interface UpdateJobRequest {
  name?: string;
  description?: string;
  cronExpression?: string;
  enabled?: boolean;
  maxRetries?: number;
  timeoutSeconds?: number;
}

/**
 * Job list response with pagination
 * Matches backend JobListResponse DTO
 */
export interface JobListResponse {
  jobs: Job[];              // Backend uses 'jobs', not 'content'
  totalElements: number;
  totalPages: number;
  currentPage: number;      // Backend uses 'currentPage', not 'number'
  pageSize: number;         // Backend uses 'pageSize', not 'size'
  hasNext: boolean;
  hasPrevious: boolean;
}

