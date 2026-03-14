import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClusterStatus } from '../models/cluster.model';

/**
 * Cluster Service - handles cluster status API calls
 */
@Injectable({
  providedIn: 'root'
})
export class ClusterService {
  private readonly baseUrl = '/cluster';

  constructor(private http: HttpClient) {}

  /**
   * Get cluster status
   */
  getClusterStatus(): Observable<ClusterStatus> {
    return this.http.get<ClusterStatus>(`${this.baseUrl}/status`);
  }

  /**
   * Get leader node ID
   */
  getLeaderNodeId(): Observable<{ leaderId: string | null }> {
    return this.http.get<{ leaderId: string | null }>(`${this.baseUrl}/leader`);
  }
}

