# Employee Time Planner

A Java Timefold and Bootstrap employee schedule planner with a simple React frontend, fully containerized with Docker.

## Features

- **Backend**: Spring Boot application with Timefold Solver for constraint-based scheduling
- **Frontend**: React application with Bootstrap styling
- **Constraint Solving**: Automatically assigns employees to shifts while avoiding conflicts
- **Dockerized**: Complete Docker setup for easy deployment

## Architecture

- **Backend**: Java 17 + Spring Boot + Timefold Solver
- **Frontend**: React 18 + Bootstrap 5
- **Containerization**: Docker + Docker Compose

## Prerequisites

- Docker and Docker Compose (for containerized deployment)
- OR Java 17+ and Maven (for local backend development)
- OR Node.js 18+ (for local frontend development)

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
npm start
```

The frontend will start on http://localhost:3000

## Usage

1. **Add Employees**: Enter employee names in the Employees section
2. **Add Shifts**: Create shifts with start time, end time, and required role
3. **Solve Schedule**: Click "Solve Schedule" to generate an optimized assignment
4. **View Results**: See the optimized schedule with assigned shifts and any conflicts

## API Endpoints

- `GET /api/employees` - Get all employees
- `POST /api/employees` - Add a new employee
- `DELETE /api/employees` - Clear all employees
- `GET /api/shifts` - Get all shifts
- `POST /api/shifts` - Add a new shift
- `DELETE /api/shifts` - Clear all shifts
- `GET /api/schedule` - Get current schedule
- `POST /api/solve` - Solve the scheduling problem

## Constraints

The scheduler implements the following constraints:

### Hard Constraints
- **No Overlapping Shifts**: An employee cannot work two shifts at the same time

### Soft Constraints
- **Minimize Unassigned Shifts**: Maximize the number of assigned shifts

## Technologies Used

### Backend
- Spring Boot 3.2.0
- Timefold Solver 1.5.0
- Java 17
- Maven

### Frontend
- React 18.2.0
- Bootstrap 5.3.2
- Axios for API calls
- React Scripts

### DevOps
- Docker
- Docker Compose
- Nginx (for frontend serving)

## License

MIT