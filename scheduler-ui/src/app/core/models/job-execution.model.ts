/**
 * Job execution model matching backend JobExecutionResponse DTO
 */
export interface JobExecution {
  id: number;
  jobId: number;
  jobName: string;
  status: ExecutionStatus;
  nodeId: string;
  retryAttempt: number;
  fencingToken: number;
  startTime: string;
  endTime: string | null;
  errorMessage: string | null;
}

/**
 * Execution status enum matching backend ExecutionStatus
 */
export enum ExecutionStatus {
  SCHEDULED = 'SCHEDULED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  TIMEOUT = 'TIMEOUT',
  CANCELLED = 'CANCELLED',
  SKIPPED = 'SKIPPED'
}

/**
 * Execution history response with pagination
 * Matches backend ExecutionHistoryResponse DTO
 */
export interface ExecutionHistoryResponse {
  executions: JobExecution[];  // Backend uses 'executions', not 'content'
  totalElements: number;
  totalPages: number;
  currentPage: number;         // Backend uses 'currentPage', not 'number'
  pageSize: number;            // Backend uses 'pageSize', not 'size'
  hasNext: boolean;
  hasPrevious: boolean;
}

