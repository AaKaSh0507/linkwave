# Linkwave Frontend

Production-grade frontend scaffolding for the Linkwave realtime chat application.

## Technology Stack

- **Vue**: 3.5.13
- **Vite**: 5.4.11
- **Vuetify**: 3.7.5 (Material Design Component Framework)
- **Pinia**: 2.2.8 (State Management)
- **Axios**: 1.7.9 (HTTP Client for REST APIs)
- **Build Tool**: Vite
- **Package Manager**: npm

## Dependencies Included

### Core Framework
- `vue` - Progressive JavaScript framework
- `vuetify` - Material Design component library
- `pinia` - State management for Vue 3
- `axios` - Promise-based HTTP client for REST API calls
- `@mdi/font` - Material Design Icons

### Development Tools
- `@vitejs/plugin-vue` - Vue 3 plugin for Vite
- `vite` - Next-generation frontend tooling
- `vite-plugin-vuetify` - Vuetify plugin for Vite

## Project Structure

```
frontend/
├── package.json              # Dependencies and scripts
├── vite.config.js            # Vite configuration
├── index.html                # HTML entry point
├── Dockerfile                # Multi-stage Docker build
├── nginx.conf                # Nginx configuration for production
├── public/                   # Static assets
└── src/
    ├── main.js               # Application entrypoint (Vuetify + Pinia configured)
    ├── App.vue               # Root component
    ├── views/                # Page components (REST-resource aligned)
    │   ├── LoginView.vue     # Login page scaffold
    │   └── ChatView.vue      # Chat interface scaffold
    ├── components/           # Reusable components (ready for REST data)
    ├── store/                # Pinia stores (ready for REST-backed state)
    ├── utils/                # Utility functions (axios config ready)
    └── assets/               # Images, fonts, etc.
```

## Prerequisites

- **Node.js**: 20.x or later
- **npm**: 10.x or later
- **Docker** (for containerized builds)

## Development

### Install Dependencies

```bash
npm install
```

### Run Development Server

```bash
npm run dev
```

The application will start on `http://localhost:3000`

Hot module replacement (HMR) is enabled for instant updates during development.

### Build for Production

```bash
npm run build
```

Optimized production files will be generated in the `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t linkwave-frontend:latest .
```

### Run Docker Container

```bash
docker run -p 80:80 linkwave-frontend:latest
```

The application will be served via Nginx on `http://localhost`

## Configuration

### Vite Configuration

Located in `vite.config.js`:
- Dev server port: 3000
- API proxy: `/api` → `http://localhost:8080`
- Path alias: `@` → `src/`

### Nginx Configuration

Located in `nginx.conf`:
- Serves static files from `/usr/share/nginx/html`
- Proxies `/api` requests to backend
- Enables gzip compression
- SPA routing support

## Available Views

### LoginView
Scaffold for user authentication interface with:
- Username input field
- Password input field
- Login button
- Material Design styling via Vuetify

### ChatView
Scaffold for realtime chat interface with:
- Conversation list sidebar
- Message display area
- Message input field
- Responsive layout (mobile & desktop)

## State Management

Pinia is configured for centralized state management. Create stores in `src/store/` directory.

## Component Library

Vuetify 3 is pre-configured with:
- Material Design Icons (@mdi/font)
- Full component library auto-import
- Light theme (default)
- Responsive grid system

## Development Workflow

1. Create components in `src/components/`
2. Add views in `src/views/` (aligned to REST resources)
3. Manage state with Pinia stores in `src/store/`
4. Configure axios for REST API calls in `src/utils/`
5. Configure routing (to be implemented)

### REST API Integration Ready
- **Axios** is configured for HTTP requests
- **Pinia** stores ready for REST-backed state management
- **Views** structured to align with resource contexts
- **API proxy** configured in Vite for `/api` → `http://localhost:8080`

### Current Status
- ✅ Vue 3 + Vuetify + Pinia configured
- ✅ Axios installed for REST API calls
- ✅ Views scaffolded (LoginView, ChatView)
- ✅ Build succeeds with zero business logic
- ✅ API proxy ready for backend integration
- ⏳ No routing logic implemented
- ⏳ No axios service layer implemented
- ⏳ No Pinia stores implemented
- ⏳ No authentication flows implemented

## Next Steps

1. Implement Vue Router for navigation
2. Create Pinia stores for state management
3. Build reusable UI components
4. Add form validation
5. Implement API service layer
6. Add WebSocket client for realtime features
7. Implement authentication flow
8. Add error handling and loading states

## API Integration

The Vite dev server proxies `/api` requests to the backend at `http://localhost:8080`. Update the proxy configuration in `vite.config.js` if your backend runs on a different port.

## License

Proprietary - Linkwave Project
