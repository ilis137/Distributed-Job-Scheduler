# Scheduler UI - Angular Frontend

Angular 17 frontend for the Distributed Job Scheduler system.

## Features

- **Job Management Dashboard**: View, create, edit, and delete jobs
- **Job Lifecycle Controls**: Pause, resume, cancel, and trigger jobs
- **Execution History**: View detailed execution history for each job
- **Cluster Visualization**: Monitor cluster status, leader election, and node health
- **Real-time Updates**: Auto-refresh for cluster status and job lists
- **Responsive Design**: Material Design UI with responsive layout

## Prerequisites

- Node.js 18+ and npm 9+
- Angular CLI 17+
- Backend API running on `http://localhost:8080`

## Installation

```bash
# Install dependencies
npm install

# Install Angular CLI globally (if not already installed)
npm install -g @angular/cli@17
```

## Development Server

```bash
# Start development server
npm start

# Or with Angular CLI
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any source files.

The development server uses a proxy configuration to forward API requests to `http://localhost:8080` to avoid CORS issues.

## Build

```bash
# Development build
npm run build

# Production build
npm run build:prod
```

The build artifacts will be stored in the `dist/` directory.

## Project Structure

```
scheduler-ui/
├── src/
│   ├── app/
│   │   ├── core/                    # Core services, models, interceptors
│   │   │   ├── models/              # TypeScript interfaces (DTOs)
│   │   │   ├── services/            # API services
│   │   │   └── interceptors/        # HTTP interceptors
│   │   ├── features/                # Feature modules
│   │   │   ├── jobs/                # Job management components
│   │   │   │   ├── job-list/
│   │   │   │   ├── job-form/
│   │   │   │   └── job-detail/
│   │   │   └── cluster/             # Cluster monitoring components
│   │   │       └── cluster-status/
│   │   ├── app.component.*          # Root component
│   │   ├── app.config.ts            # Application configuration
│   │   └── app.routes.ts            # Routing configuration
│   ├── environments/                # Environment configurations
│   ├── assets/                      # Static assets
│   └── styles.scss                  # Global styles
├── angular.json                     # Angular CLI configuration
├── package.json                     # npm dependencies
└── tsconfig.json                    # TypeScript configuration
```

## API Integration

The frontend connects to the backend REST API at:
- **Development**: `http://localhost:8080/api/v1` (via proxy)
- **Production**: `/api/v1` (relative URL)

### API Endpoints Used

- `GET /jobs` - List all jobs with pagination
- `POST /jobs` - Create new job
- `GET /jobs/{id}` - Get job details
- `PATCH /jobs/{id}` - Update job
- `DELETE /jobs/{id}` - Delete job
- `POST /jobs/{id}/pause` - Pause job
- `POST /jobs/{id}/resume` - Resume job
- `POST /jobs/{id}/trigger` - Trigger job immediately
- `GET /executions/job/{jobId}` - Get execution history
- `GET /cluster/status` - Get cluster status

## Configuration

### Environment Variables

Edit `src/environments/environment.ts` for development:

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  pollingInterval: 5000 // Auto-refresh interval in ms
};
```

Edit `src/environments/environment.prod.ts` for production.

## Technologies Used

- **Angular 17** - Frontend framework
- **Angular Material** - UI component library
- **RxJS** - Reactive programming
- **TypeScript** - Type-safe JavaScript
- **SCSS** - Styling

## Development Notes

- Uses standalone components (Angular 17+ feature)
- Functional HTTP interceptors
- Lazy-loaded routes for better performance
- Material Design theming
- Responsive grid layouts

## Troubleshooting

### CORS Issues

If you encounter CORS errors, make sure:
1. The backend is running on `http://localhost:8080`
2. The proxy configuration in `src/proxy.conf.json` is correct
3. You're using `npm start` (not `ng serve` directly)

### Backend Connection

If the frontend can't connect to the backend:
1. Verify the backend is running: `http://localhost:8080/actuator/health`
2. Check the API base URL in `src/environments/environment.ts`
3. Check browser console for error messages

## License

MIT

