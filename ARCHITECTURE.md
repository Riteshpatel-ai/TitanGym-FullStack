# TitanGym Full Stack

TitanGym is a gym-focused e-commerce application. The main product is a React single-page storefront backed by a Spring Boot REST API and PostgreSQL. It supports registration/login, a JWT-protected product catalog, search, a per-user cart, payment-session creation, and a catalog-aware Coach AI assistant.

The repository also contains an independent Flask/PyTorch image-search prototype under `ml_backend/`. That prototype is not used by the React application or the Spring Boot API at present.

## Architecture at a glance

```text
Browser
  |
  | React Router + React Context + Axios
  v
Frontend (React 19, port 3000)
  |
  | JSON REST requests with Authorization: Bearer <JWT>
  v
Backend (Spring Boot 3.4.5, Java 17, port 8082)
  |-- Controllers: HTTP boundary and DTO validation
  |-- Services: auth, products, cart, Coach AI, payments
  |-- Repositories: Spring Data JPA
  |-- Entities: users, products, cart_items
  |-- Security: BCrypt + stateless JWT filter
  v
PostgreSQL (ecommerce_db, port 5432)

External services:
  - Groq-compatible chat-completions API (optional Coach AI provider)
  - Razorpay (INR order creation)
  - Stripe (checkout-session creation; current service uses CAD)

Separate prototype:
  Browser/client -> Flask image upload/search routes -> ResNet50 embeddings -> FAISS index
```

## Repository map

| Path | Responsibility |
| --- | --- |
| `frontend/` | React storefront, routing, pages, reusable UI, auth/cart contexts |
| `backend/` | Spring Boot commerce API |
| `backend/src/main/java/com/titangym/ecommerce/controller/` | REST endpoints |
| `backend/src/main/java/com/titangym/ecommerce/service/` | Business logic and external integrations |
| `backend/src/main/java/com/titangym/ecommerce/model/` | JPA entities and authenticated principal |
| `backend/src/main/java/com/titangym/ecommerce/repository/` | Spring Data repositories |
| `backend/src/main/java/com/titangym/ecommerce/dto/` | Request/response payloads |
| `backend/src/main/java/com/titangym/ecommerce/mapper/` | Entity-to-DTO conversion |
| `backend/src/main/java/com/titangym/ecommerce/config/` | CORS, security filter chain, JWT filter |
| `backend/src/main/java/com/titangym/ecommerce/exception/` | Domain exceptions and global HTTP error handling |
| `backend/src/main/resources/application.properties` | Runtime configuration, supplied through environment variables |
| `ml_backend/` | Standalone image upload and FAISS similarity-search prototype |
| `docker-compose.yml` | Backend, frontend, and PostgreSQL orchestration |
| `request.http` | Manual HTTP request examples, if present for local API exploration |

Generated or local-only directories such as `frontend/node_modules/`, `frontend/build/`, and `backend/target/` are build artifacts and are not application source.

## Runtime components

### Frontend

The frontend is a Create React App project using React 19, React Router, Axios, Tailwind CSS, and `react-icons`.

- Entry point: `frontend/src/index.js`
- Root router: `frontend/src/App.js`
- Axios client: `frontend/src/api/axios.js`
- Authentication state: `frontend/src/context/AuthContext.jsx`
- Cart state and API operations: `frontend/src/context/CartContext.jsx`
- Protected-route guard: `frontend/src/routes/PrivateRoute.jsx`
- Global styling: `frontend/src/index.css`

`index.js` wraps the app in `AuthProvider` and `CartProvider`. `AuthContext` stores the JWT in browser `localStorage` under `authToken`, checks its `exp` claim, and exposes `login`, `logout`, `loading`, and `isAuthenticated`. `CartContext` loads the authenticated user’s cart and exposes add, update, delete, and subtotal operations.

### Frontend routes

| Route | Component | Access |
| --- | --- | --- |
| `/login` | `Login` | Public |
| `/register` | `Register` | Public |
| `/` | `Home` | JWT required |
| `/coach-ai` | `Home` with Coach AI panel | JWT required by frontend route |
| `/products` | `ProductList` | JWT required |
| `/product/:id` | `ProductDetailPage` | JWT required |
| `/search?q=...` | `SearchResults` | JWT required |
| `/cart` | `Cart` | JWT required |
| `/checkout` | `CheckOut` | JWT required |
| `/success` | `CheckOutResultPage(success=true)` | JWT required |
| `/cancel` | `CheckOutResultPage(success=false)` | JWT required |

`Layout` supplies the shared navigation/product-shell UI. Product browsing is paginated with a default page size of 10. The UI formats catalog prices as INR using the frontend currency helper, while the backend also converts product prices using a fixed `1 USD = 83 INR` rate.

### Backend layering

The backend is a conventional Spring MVC/JPA application:

1. **Controllers** receive HTTP requests, bind DTOs, and delegate.
2. **Services** implement business rules and call repositories/external APIs.
3. **Repositories** extend `JpaRepository`.
4. **Entities** map to PostgreSQL tables.
5. **Mappers** prevent controllers from returning persistence entities directly.
6. **`GlobalExceptionHandler`** maps validation and domain/external errors to JSON responses.

The application starts from `backend/src/main/java/com/titangym/ecommerce/EcommerceApplication.java`.

## API contract

All endpoints use the `/api/v1` prefix. Except where noted, the backend security configuration requires a valid JWT in `Authorization: Bearer <token>`.

### Authentication

| Method | Endpoint | Purpose | Response |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Create a user after bean validation and duplicate username/email checks | `201`, `UserResponseDTO` |
| `POST` | `/api/v1/auth/login` | Authenticate by username/email identifier and password | `200`, `{ "token": "..." }` |

Passwords are BCrypt-hashed. Login creates a stateless JWT whose subject is the username. `JwtFilter` extracts and validates the token on each request, then places a `UserPrincipal` in the Spring Security context.

### Products

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/products?page=0&size=10&search=` | Filter, alphabetically sort, and paginate gym-relevant products |
| `GET` | `/api/v1/products/{id}` | Read one gym-relevant product |
| `POST` | `/api/v1/products` | Create a product |
| `PUT` | `/api/v1/products/{id}` | Update a product |
| `DELETE` | `/api/v1/products/{id}` | Delete a product and remove its cart references |

The list response is an object with `products`, `currentPage`, `totalItems`, and `totalPages`. `GymCatalogPolicy` determines relevance by searching product name, description, and category for gym/fitness keywords. It also ranks recommendations by matching query tokens.

### Cart

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/cart` | List the authenticated user’s cart |
| `POST` | `/api/v1/cart/add` | Add `{ productId, quantity }`; existing product entries are updated |
| `PATCH` | `/api/v1/cart/{cartItemId}` | Set a cart quantity |
| `DELETE` | `/api/v1/cart/{cartItemId}` | Delete a cart item |

Cart ownership is checked in `CartService`. Quantities are clamped to at least one and checked against product stock. Cart responses contain the cart item and mapped product data used by the frontend subtotal calculation.

### Coach AI

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/coach/chat` | Accept `{ "message": "..." }` and return `{ "reply": "...", "recommendations": ["..."] }` |

`CoachAiService` first loads and filters the product catalog, ranks up to three recommendations, and then:

- calls the configured Groq-compatible chat-completions endpoint when `GROQ_API_KEY` is present;
- otherwise returns a deterministic fallback response;
- restricts the system prompt to gym, fitness, nutrition, recovery, and workout products.

The frontend renders the reply and recommendation names in `CoachChatPanel`.

### Payments

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/payment/create-order` | Create a Razorpay order from product names, prices in paise, and quantities |
| `POST` | `/api/v1/stripe/create-checkout-session` | Create a Stripe hosted checkout session |

Razorpay calculates the total server-side and returns the public key ID, order ID, amount, currency, and receipt. Stripe receives line-item data from the request and returns a checkout URL.

Important implementation detail: the current `StripeService` sets currency to `cad`, while the storefront and Razorpay path are INR-oriented. Treat Stripe as a separate/legacy payment path unless this currency mismatch is intentionally fixed.

## Data model

Hibernate creates/updates the schema because `spring.jpa.hibernate.ddl-auto=update`.

```text
users
  id (identity primary key)
  username (unique, required)
  password (required, BCrypt hash)
  email (unique, required)

products
  id (identity primary key)
  name
  description (up to 1000 characters)
  price (BigDecimal)
  imageUrl
  category
  quantity (stock)

cart_items
  id (identity primary key)
  user_id -> users
  product_id -> products
  quantity
```

There are no checked-in Flyway/Liquibase migrations. The schema is inferred and maintained by Hibernate at startup, so production schema changes should be handled carefully before changing `ddl-auto`.

## Request flows

### Login and protected request

1. User submits credentials in `frontend/src/pages/Login.jsx`.
2. Frontend calls `POST /api/v1/auth/login`.
3. `AuthService` delegates credentials to Spring Security.
4. `JWTService` creates a signed token.
5. Frontend stores the token in `localStorage`.
6. `PrivateRoute` allows protected pages while the token is valid.
7. Protected API requests include the bearer token.
8. `JwtFilter` validates the token and loads the user.

### Browse and add to cart

1. `ProductGrid` calls the paginated product endpoint.
2. `ProductService` filters through `GymCatalogPolicy`, sorts, paginates, maps DTOs, and converts prices.
3. `ProductCard`/`ProductDetailPage` invokes cart actions.
4. `CartContext` calls the cart endpoint with the current JWT.
5. `CartService` validates the product, user, stock, and cart ownership before saving.

### Coach AI

1. `Home` renders `CoachChatPanel`.
2. The panel posts the user message to `/api/v1/coach/chat`.
3. The service derives catalog recommendations.
4. With a Groq key, it sends a catalog-constrained prompt to the external API.
5. Without a key, it returns the local fallback reply.
6. The response is displayed with recommendation chips.

### Checkout

The checkout page gathers the current cart line items, sends payment input to the selected backend payment endpoint, and redirects/opens the returned provider URL or order flow. Payment provider callbacks/webhooks and an order table are not implemented in the current source, so the application creates provider sessions/orders but does not persist a completed order in its own database.

## Configuration

### Backend environment variables

Set these in the shell, a local ignored `.env`/`.env.docker`, or your deployment secret store. Never commit real values.

| Variable | Required | Meaning |
| --- | --- | --- |
| `DB_URL` | Yes | JDBC URL, for example `jdbc:postgresql://localhost:5432/ecommerce_db` |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Strong signing secret; use a sufficiently long random value |
| `JWT_EXPIRATION` | Yes | Token lifetime in milliseconds |
| `STRIPE_SECRET_KEY` | Yes at startup | Stripe server secret; required by `StripeService` |
| `RAZORPAY_KEY_ID` | Optional | Razorpay key ID; required for Razorpay checkout |
| `RAZORPAY_KEY_SECRET` | Optional | Razorpay server secret |
| `GROQ_API_KEY` | Optional | Enables remote Coach AI; no key uses fallback mode |
| `GROQ_MODEL` | Optional | Defaults to `llama-3.1-70b-versatile` |
| `GROQ_API_URL` | Optional | Defaults to Groq’s OpenAI-compatible chat URL |

`backend/src/main/resources/application.properties` also sets port `8082`, PostgreSQL dialect, `ddl-auto=update`, SQL logging, and debug logging.

### Frontend environment variables

| Variable | Meaning |
| --- | --- |
| `REACT_APP_API_URL` | Backend base URL; defaults to `http://localhost:8082` |
| `REACT_APP_FRONTEND_BASE_URL` | Optional frontend base URL used by deployment configuration |

Create React App embeds `REACT_APP_*` values at build time. Do not put private API keys in frontend environment files. The checked-in `.env.production` currently points at a public IP/website deployment and should be replaced with environment-specific values for a new deployment.

## Local development

### Prerequisites

- Java 17
- Maven (or the Maven wrapper under `backend/`)
- Node.js and npm
- PostgreSQL 14+ recommended
- Optional: Razorpay, Stripe, and Groq credentials

### Start PostgreSQL

Create a database named `ecommerce_db`, then export the backend variables:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/ecommerce_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<local-password>"
$env:JWT_SECRET = "<long-random-secret>"
$env:JWT_EXPIRATION = "86400000"
$env:STRIPE_SECRET_KEY = "<stripe-secret>"
# Optional integrations:
$env:RAZORPAY_KEY_ID = "<razorpay-key-id>"
$env:RAZORPAY_KEY_SECRET = "<razorpay-secret>"
$env:GROQ_API_KEY = "<groq-key>"
```

### Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8082`.

### Start the frontend

```powershell
cd frontend
npm install
$env:REACT_APP_API_URL = "http://localhost:8082"
npm start
```

The development UI listens on `http://localhost:3000`.

### Docker Compose

`docker-compose.yml` defines `backend`, `frontend`, and `db`, exposes ports `8082`, `3000`, and `5432`, and persists PostgreSQL in the `postgres_data` volume. It expects a root `.env.docker` file, which is not currently checked in. Create it locally with the variables required by the backend and database before running:

```powershell
docker compose up --build
```

The backend image expects a pre-built JAR in `backend/target/` because `backend/Dockerfile` copies `target/*.jar`. Build it first with `.\mvnw.cmd package -DskipTests`, or change the Dockerfile to a multi-stage Maven build.

The frontend image builds the React app and serves it through Nginx. `frontend/nginx.conf` falls back to `index.html` for client-side routes.

## Tests and validation

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

The checked-in test suite currently contains a Spring context-load test in `backend/src/test/java/com/titangym/ecommerce/EcommerceApplicationTests.java`. There are no checked-in frontend unit tests beyond the Create React App testing dependencies.

Frontend:

```powershell
cd frontend
npm test
npm run build
```

## Image-search prototype (`ml_backend`)

This directory is a separate Python prototype:

- `model.py` loads a pretrained ResNet-50 and removes its classifier, producing 2048-dimensional image embeddings.
- `bulid_index.py` reads `.jpg` files from `ml_backend/static`, builds a FAISS `IndexFlatL2`, and writes `embeddings/index.faiss` plus `embeddings/ids.pkl`.
- `upload.py` exposes `POST /api/v1/upload_image`, returning an embedding vector and base64 preview.
- `search.py` exposes `POST /api/v1/search-image`, accepts an uploaded image, and returns the five nearest indexed IDs.
- `app.py` creates the Flask app and enables permissive CORS.

Current integration caveats:

1. `app.py` registers `search_bp` but does not import it, so the Flask app will fail at startup until that import/registration is corrected.
2. No Python dependency manifest or Dockerfile is checked in for this prototype.
3. The React frontend does not call these endpoints.
4. The FAISS index and relative paths must exist from the process working directory.
5. CORS is configured for all origins and should be restricted before production use.

## Known behavior and maintenance notes

- Security is stateless JWT + BCrypt; there are no roles or admin-specific authorities. Product write endpoints are authenticated but not role-restricted.
- `/api/v1/coach/**` is `permitAll()` in `SecurityConfig`, although the React route is wrapped in `PrivateRoute`.
- `debug=true` and SQL logging are enabled in `application.properties`; disable them in production.
- CORS allows localhost patterns and one historical S3 website origin. Update the allow-list for the actual deployment.
- Product price conversion uses a fixed exchange rate in `ProductService`; it is not a live currency conversion service.
- Cart operations validate stock but do not decrement inventory when a payment is completed.
- There is no order/payment record, webhook verification, refund flow, or inventory reservation in the current data model.
- Stripe uses CAD in the current service, while the product UI and Razorpay use INR.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for development but is not a substitute for versioned production migrations.
- The previous README contained payment credentials. Credentials must not be documented in source control; rotate any values that may have been exposed and use secret storage.

## Where to look for common questions

| Question | Start here |
| --- | --- |
| How does login work? | `frontend/src/pages/Login.jsx`, `frontend/src/context/AuthContext.jsx`, `backend/.../AuthController.java`, `AuthService.java`, `JwtFilter.java` |
| Where is the API base URL? | `frontend/src/api/axios.js`, `frontend/.env.production` |
| How are products filtered? | `backend/.../ProductService.java`, `GymCatalogPolicy.java` |
| How does cart ownership work? | `frontend/src/context/CartContext.jsx`, `CartController.java`, `CartService.java` |
| How does Coach AI work without a provider key? | `CoachAiService.java`, `GymCatalogPolicy.java`, `CoachChatPanel.jsx` |
| Which payment provider is active? | `CheckOut.jsx`, `RazorpayController.java`, `RazorpayService.java`, `StripeController.java`, `StripeService.java` |
| How is the database created? | JPA entities plus `application.properties`; there are no migration scripts |
| How is the app containerized? | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` |
| Is image search part of the storefront? | No; inspect `ml_backend/` for the separate prototype |

## License and contributions

No license file or contribution policy is currently defined in the repository. Add those files before distributing the project or accepting external contributions.
