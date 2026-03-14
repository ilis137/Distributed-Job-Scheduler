import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/jobs',
    pathMatch: 'full'
  },
  {
    path: 'jobs',
    loadComponent: () => import('./features/jobs/job-list/job-list.component').then(m => m.JobListComponent)
  },
  {
    path: 'jobs/create',
    loadComponent: () => import('./features/jobs/job-form/job-form.component').then(m => m.JobFormComponent)
  },
  {
    path: 'jobs/:id',
    loadComponent: () => import('./features/jobs/job-detail/job-detail.component').then(m => m.JobDetailComponent)
  },
  {
    path: 'jobs/:id/edit',
    loadComponent: () => import('./features/jobs/job-form/job-form.component').then(m => m.JobFormComponent)
  },
  {
    path: 'cluster',
    loadComponent: () => import('./features/cluster/cluster-status/cluster-status.component').then(m => m.ClusterStatusComponent)
  },
  {
    path: '**',
    redirectTo: '/jobs'
  }
];

