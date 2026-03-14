import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { ClusterService } from '../../../core/services/cluster.service';
import { ClusterStatus, NodeStatus } from '../../../core/models/cluster.model';
import { interval, Subject, takeUntil, startWith, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-cluster-status',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './cluster-status.component.html',
  styleUrls: ['./cluster-status.component.scss']
})
export class ClusterStatusComponent implements OnInit, OnDestroy {
  clusterStatus: ClusterStatus | null = null;
  loading = false;
  error: string | null = null;
  displayedColumns = ['nodeId', 'status', 'isLeader', 'epoch', 'lastHeartbeat', 'uptime'];

  private destroy$ = new Subject<void>();

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    // Auto-refresh cluster status every 5 seconds
    interval(environment.pollingInterval)
      .pipe(
        startWith(0),
        switchMap(() => this.clusterService.getClusterStatus()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (status) => {
          this.clusterStatus = status;
          this.loading = false;
        },
        error: (err) => {
          this.error = err.message;
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

