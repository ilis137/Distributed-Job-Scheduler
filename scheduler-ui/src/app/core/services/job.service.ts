import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Job, CreateJobRequest, UpdateJobRequest, JobListResponse } from '../models/job.model';

/**
 * Job Service - handles all job-related API calls
 */
@Injectable({
  providedIn: 'root'
})
export class JobService {
  private readonly baseUrl = '/jobs';

  constructor(private http: HttpClient) {}

  /**
   * Get all jobs with pagination
   */
  getJobs(page: number = 0, size: number = 20, status?: string): Observable<JobListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<JobListResponse>(this.baseUrl, { params });
  }

  /**
   * Get job by ID
   */
  getJobById(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.baseUrl}/${id}`);
  }

  /**
   * Create new job
   */
  createJob(request: CreateJobRequest): Observable<Job> {
    return this.http.post<Job>(this.baseUrl, request);
  }

  /**
   * Update existing job
   */
  updateJob(id: number, request: UpdateJobRequest): Observable<Job> {
    return this.http.patch<Job>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Delete job
   */
  deleteJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Pause job
   */
  pauseJob(id: number): Observable<Job> {
    return this.http.post<Job>(`${this.baseUrl}/${id}/pause`, {});
  }

  /**
   * Resume job
   */
  resumeJob(id: number): Observable<Job> {
    return this.http.post<Job>(`${this.baseUrl}/${id}/resume`, {});
  }

  /**
   * Cancel job
   */
  cancelJob(id: number): Observable<Job> {
    return this.http.post<Job>(`${this.baseUrl}/${id}/cancel`, {});
  }

  /**
   * Trigger job immediately
   */
  triggerJob(id: number): Observable<Job> {
    return this.http.post<Job>(`${this.baseUrl}/${id}/trigger`, {});
  }
}

