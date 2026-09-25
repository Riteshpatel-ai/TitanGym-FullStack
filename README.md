# TitanGym

> A gym-focused e-commerce platform with personalized product guidance from Coach AI.

TitanGym is a full-stack shopping application for fitness products such as strength equipment, recovery gear, nutrition products, and workout accessories. It combines a React storefront, a Spring Boot REST API, PostgreSQL persistence, JWT authentication, cart management, and payment-provider integrations.

## Features

- User registration and login with BCrypt passwords and stateless JWT authentication
- Protected React routes for the storefront and checkout
- Gym-product catalog with search, filtering, sorting, and pagination
- Product details and responsive product cards
- Per-user shopping cart with quantity and stock validation
- Coach AI assistant for catalog-aware fitness product recommendations
- Optional Groq-compatible AI integration with a local fallback response
- Razorpay order creation for INR checkout
- Stripe checkout-session integration
- Docker Compose setup for frontend, backend, and PostgreSQL
- Separate image-search prototype using ResNet-50 embeddings and FAISS

## Architecture

```text
React 19 + React Router + Axios
              |
              | JSON REST API + Bearer JWT
              v
Spring Boot 3.4.5 + Java 17
              |
              +-- Spring Security / JWT
              +-- Services and REST controllers
              +-- Spring Data JPA
              v
          PostgreSQL

External integrations:
  Groq-compatible API | Razorpay | Stripe
```

The detailed source-based architecture reference is available in [ARCHITECTURE.md](./ARCHITECTURE.md).

## Technology stack

### Frontend

- React 19
- React Router
- Axios
- Tailwind CSS
- Create React App

### Backend

- Java 17
- Spring Boot 3.4.5
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- JJWT
- Razorpay Java SDK
- Stripe Java SDK

### Additional prototype

- Python
- Flask
- PyTorch / Torchvision
- ResNet-50
- FAISS

## Project structure

```text
TitanGym-FullStack-clone/
├── frontend/                 # React storefront
├── backend/                  # Spring Boot REST API
├── ml_backend/               # Independent image-search prototype
├── docker-compose.yml        # Frontend, backend, and PostgreSQL services
├── ARCHITECTURE.md           # Detailed architecture and implementation guide
└── README.md                # GitHub project overview
```

## Quick start

### Prerequisites

- Java 17
- Maven
- Node.js and npm
- PostgreSQL

### 1. Configure the backend

Create a PostgreSQL database named `ecommerce_db`, then set the required environment variables.

PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/ecommerce_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<database-password>"
$env:JWT_SECRET = "<long-random-secret>"
$env:JWT_EXPIRATION = "86400000"
$env:STRIPE_SECRET_KEY = "<stripe-secret-key>"
```

Optional integrations:

```powershell
$env:RAZORPAY_KEY_ID = "<razorpay-key-id>"
$env:RAZORPAY_KEY_SECRET = "<razorpay-secret>"
$env:GROQ_API_KEY = "<groq-api-key>"
$env:GROQ_MODEL = "llama-3.1-70b-versatile"
```

### 2. Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8082`.

### 3. Start the frontend

```powershell
cd frontend
npm install
$env:REACT_APP_API_URL = "http://localhost:8082"
npm start
```

The storefront runs at `http://localhost:3000`.

## Docker Compose

Create a local `.env.docker` file with the database and backend variables, then run:

```powershell
docker compose up --build
```

Services:

| Service | URL/Port |
| --- | --- |
| Frontend | `http://localhost:3000` |
| Backend API | `http://localhost:8082` |
| PostgreSQL | `localhost:5432` |

The backend Dockerfile expects a packaged JAR in `backend/target/`. Build it first if required:

```powershell
cd backend
.\mvnw.cmd package -DskipTests
```

## Main API areas

| Area | Endpoints |
| --- | --- |
| Authentication | `/api/v1/auth/register`, `/api/v1/auth/login` |
| Products | `/api/v1/products`, `/api/v1/products/{id}` |
| Cart | `/api/v1/cart`, `/api/v1/cart/add`, `/api/v1/cart/{cartItemId}` |
| Coach AI | `/api/v1/coach/chat` |
| Razorpay | `/api/v1/payment/create-order` |
| Stripe | `/api/v1/stripe/create-checkout-session` |

Most endpoints require `Authorization: Bearer <jwt-token>`. Registration and login are public. The Coach AI endpoint is currently permitted by backend security configuration.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret |
| `JWT_EXPIRATION` | JWT lifetime in milliseconds |
| `STRIPE_SECRET_KEY` | Stripe server secret |
| `RAZORPAY_KEY_ID` | Razorpay public key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay server secret |
| `GROQ_API_KEY` | Optional Coach AI provider key |
| `GROQ_MODEL` | Optional Groq model name |
| `REACT_APP_API_URL` | Frontend backend base URL |

Never commit real credentials, payment keys, database passwords, or JWT secrets.

## Testing

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm test
npm run build
```

## Important notes

- Hibernate currently manages schema changes with `spring.jpa.hibernate.ddl-auto=update`.
- Completed orders are not persisted in a dedicated order table.
- Payment webhooks, refunds, and inventory deduction after payment are not implemented.
- The Stripe service currently uses CAD while the storefront and Razorpay flow are INR-oriented.
- `ml_backend/` is a separate prototype and is not connected to the React storefront.
- Review [ARCHITECTURE.md](./ARCHITECTURE.md) for implementation details, request flows, database entities, deployment notes, and known gaps.

## Contributing

1. Create a feature branch.
2. Keep secrets in local environment variables.
3. Run the relevant backend and frontend checks.
4. Open a pull request with a clear description of the change.

## License

No license file is currently included in the repository.
