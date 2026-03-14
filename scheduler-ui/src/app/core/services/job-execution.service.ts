import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobExecution, ExecutionHistoryResponse } from '../models/job-execution.model';

/**
 * Job Execution Service - handles execution history API calls
 */
@Injectable({
  providedIn: 'root'
})
export class JobExecutionService {
  private readonly baseUrl = '/executions';

  constructor(private http: HttpClient) {}

  /**
   * Get execution by ID
   */
  getExecutionById(id: number): Observable<JobExecution> {
    return this.http.get<JobExecution>(`${this.baseUrl}/${id}`);
  }

  /**
   * Get execution history for a specific job
   */
  getJobExecutionHistory(
    jobId: number,
    page: number = 0,
    size: number = 20
  ): Observable<ExecutionHistoryResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ExecutionHistoryResponse>(`${this.baseUrl}/job/${jobId}`, { params });
  }

  /**
   * Get all executions with pagination
   */
  getAllExecutions(
    page: number = 0,
    size: number = 20,
    status?: string
  ): Observable<ExecutionHistoryResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ExecutionHistoryResponse>(this.baseUrl, { params });
  }
}

