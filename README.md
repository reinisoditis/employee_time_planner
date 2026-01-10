# Employee Time Planner

A Java Timefold and Tailwind CSS employee schedule planner with a simple React frontend, fully containerized with Docker.

## Features

- **Backend**: Spring Boot application with Timefold Solver for constraint-based scheduling
- **Frontend**: React application with Tailwind CSS styling
- **Constraint Solving**: Automatically assigns employees to shifts while avoiding conflicts
- **Dockerized**: Complete Docker setup for easy deployment

## Architecture

- **Backend**: Java 17 + Spring Boot + Timefold Solver
- **Frontend**: React 19 + Tailwind CSS + Vite
- **Containerization**: Docker + Docker Compose

## Prerequisites

- Docker and Docker Compose (for containerized deployment)
- OR Java 17+ and Maven (for local backend development)
- OR Node.js 20+ (for local frontend development)

## Quick Start with Docker

**Note**: The Docker setup requires pre-built artifacts. Build the backend and frontend locally first:

```bash
# Build backend
cd backend
mvn clean package -DskipTests
cd ..

# Build frontend
cd frontend
npm install
npm run build
cd ..
```

Then start with Docker Compose:

```bash
docker-compose up --build
```

Access the application:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api

## Local Development

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on http://localhost:8080

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on http://localhost:5173

## Usage

1. **Add Employees**: Enter employee names in the Employees section
2. **Add Shifts**: Create shifts with start time, end time, and required role
3. **Solve Schedule**: Click "Solve Schedule" to generate an optimized assignment
4. **View Results**: See the optimized schedule with assigned shifts and any conflicts

## API Endpoints

- `GET /api` - Get all jobs
- `GET /api/{jobId}` - Get a specific job
- `GET /api/score/{jobId}` - Get score for a job
- `GET /api/indictments/{jobId}` - Get constraint indictments for a job
- `POST /api` - Submit a new schedule for solving

## Constraints

The scheduler implements the following constraints:

### Hard Constraints
- **No Overlapping Shifts**: An employee cannot work two shifts at the same time

### Soft Constraints
- **Minimize Unassigned Shifts**: Maximize the number of assigned shifts

## Technologies Used

### Backend
- Spring Boot 3.2.0
- Timefold Solver 1.27.0
- Java 17
- Maven

### Frontend
- React 19.2.0
- Tailwind CSS 4.1
- Vite 7.2
- TypeScript

### DevOps
- Docker
- Docker Compose
- Nginx (for frontend serving)

## License

MIT