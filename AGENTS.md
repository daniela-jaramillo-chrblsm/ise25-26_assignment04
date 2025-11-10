# CampusCoffee - AI Assistant Context Guide

This document provides context and guidelines for AI assistants working on the CampusCoffee project. It describes the architecture, patterns, conventions, and domain knowledge needed to implement features correctly.

## Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Coding Conventions](#coding-conventions)
- [Domain Knowledge](#domain-knowledge)
- [Common Patterns](#common-patterns)
- [Adding New Features](#adding-new-features)
- [Testing](#testing)
- [Development Workflow](#development-workflow)
- [Important Notes](#important-notes)

---

## Project Overview

**CampusCoffee** is a Spring Boot application for managing Points of Sale (POS) on university campuses. It allows users to:
- Create, read, update POS locations
- Import POS data from OpenStreetMap
- Query POS by campus location

**Technology Stack:**
- Java 21 (Temurin JDK)
- Spring Boot 3.5.7
- PostgreSQL 16
- Maven 3.9
- MapStruct (for mapping)
- Lombok (for boilerplate reduction)
- Flyway (for database migrations)
- JPA/Hibernate (for persistence)

---

## Architecture

This project follows **Hexagonal Architecture** (also known as Ports and Adapters). The architecture separates concerns into distinct layers:

### Layer Responsibilities

1. **Domain Layer** (`domain/`)
   - Core business logic and domain models
   - **Ports** (interfaces): Define contracts for external dependencies
   - **Models**: Immutable domain records (value objects)
   - **Services**: Business logic implementations
   - **Exceptions**: Domain-specific exceptions
   - **NO dependencies** on other layers (pure Java)

2. **Data Layer** (`data/`)
   - Persistence implementation (JPA entities, repositories)
   - **Adapters**: Implement domain ports (e.g., `PosDataService`)
   - **Mappers**: Convert between domain models and JPA entities
   - **Migrations**: Flyway SQL scripts
   - Depends only on domain layer

3. **API Layer** (`api/`)
   - REST controllers and DTOs
   - **Adapters**: Consume domain services
   - **Mappers**: Convert between domain models and DTOs
   - **Exception Handlers**: Translate domain exceptions to HTTP responses
   - Depends only on domain layer

4. **Application Layer** (`application/`)
   - Spring Boot configuration and startup
   - Application properties
   - System tests
   - Orchestrates all layers

### Dependency Flow

```
API Layer → Domain Layer ← Data Layer
     ↓           ↑
Application Layer
```

**Key Principle**: Domain layer has **zero dependencies** on other layers. All dependencies point **toward** the domain.

---

## Project Structure

```
├── api/                    # API layer (REST controllers, DTOs)
│   ├── controller/         # REST controllers
│   ├── dtos/               # Data Transfer Objects
│   ├── mapper/             # MapStruct mappers (domain ↔ DTO)
│   └── exceptions/         # Global exception handlers
├── application/            # Spring Boot application
│   ├── Application.java    # Main entry point
│   ├── LoadInitialData.java # Dev profile data loader
│   └── resources/
│       ├── application.yaml # Configuration
│       └── db/migration/    # Flyway migrations
├── data/                   # Data layer (persistence)
│   ├── impl/               # Port implementations
│   ├── mapper/             # MapStruct mappers (domain ↔ entity)
│   └── persistence/        # JPA entities and repositories
├── domain/                 # Domain layer (core business logic)
│   ├── model/              # Domain models (records)
│   ├── ports/              # Service interfaces (ports)
│   ├── impl/               # Service implementations
│   ├── exceptions/         # Domain exceptions
│   └── tests/              # Test fixtures
└── pom.xml                 # Maven parent POM
```

---

## Coding Conventions

### Java Version & Features
- **Java 21** with modern features:
  - Records for immutable domain models
  - Pattern matching
  - Text blocks
  - Sealed classes (if needed)

### Nullability
- Use **JSpecify annotations** (`@NonNull`, `@Nullable`)
- Domain models use `@Nullable` for optional fields (e.g., `id` before persistence)
- Service methods return `@NonNull` unless explicitly documented

### Domain Models
- Use **Java records** for immutable domain models
- Use `@Builder(toBuilder = true)` for easy modification
- Example:
  ```java
  @Builder(toBuilder = true)
  public record Pos(
      @Nullable Long id,
      @NonNull String name,
      // ...
  ) {}
  ```

### Mapping
- Use **MapStruct** for all conversions:
  - Domain ↔ DTO (in `api/mapper/`)
  - Domain ↔ Entity (in `data/mapper/`)
- Mappers are interfaces with `@Mapper(componentModel = "spring")`
- Use `@ConditionalOnMissingBean` to prevent IntelliJ warnings

### Dependency Injection
- Use **Lombok `@RequiredArgsConstructor`** for constructor injection
- Prefer constructor injection over field injection
- Services are annotated with `@Service`

### Logging
- Use **SLF4J** with Lombok `@Slf4j`
- Log levels:
  - `ERROR`: Exceptions and critical failures
  - `WARN`: Recoverable issues, constraint violations
  - `INFO`: Important business operations (create, update, import)
  - `DEBUG`: Detailed flow information

### Exception Handling
- Domain exceptions extend `RuntimeException`
- Exception names are descriptive: `PosNotFoundException`, `DuplicatePosNameException`
- Global exception handler in API layer translates to HTTP status codes:
  - `404`: Not found exceptions
  - `409`: Duplicate/conflict exceptions
  - `400`: Validation/bad request exceptions
  - `500`: Unexpected errors

---

## Domain Knowledge

### POS (Point of Sale)
A **Point of Sale** represents a location where coffee or related products can be purchased on campus. Each POS has:
- **Name**: Unique identifier (enforced by database constraint)
- **Description**: Text description
- **Type**: One of `CAFE`, `VENDING_MACHINE`, `BAKERY`, `CAFETERIA`
- **Campus**: One of `ALTSTADT`, `BERGHEIM`, `INF`
- **Address**: Street, house number (may include suffix like "21a"), postal code, city

### Address Handling
- **Domain model**: Flat structure (street, houseNumber as string "21a", postalCode, city)
- **Persistence**: Embedded `AddressEntity` with split house number (numeric + suffix)
- **Mapper responsibility**: Convert between flat and embedded structures
- House number parsing: Extract numeric part and suffix (e.g., "21a" → numeric=21, suffix='a')

### Timestamps
- `createdAt`: Set on entity creation (via `@PrePersist`)
- `updatedAt`: Set on creation and update (via `@PrePersist` and `@PreUpdate`)
- Timestamps are in **UTC** timezone
- Managed by JPA lifecycle callbacks, **not** by domain layer

### OpenStreetMap Integration
- POS can be imported from OpenStreetMap nodes
- OSM node ID is a `Long`
- Current implementation is a stub (hardcoded for node 5589879349)
- Future: Extract tags from OSM node and map to POS fields

---

## Common Patterns

### 1. Adding a New Domain Entity

**Step 1: Create Domain Model** (`domain/model/`)
```java
@Builder(toBuilder = true)
public record MyEntity(
    @Nullable Long id,
    @Nullable LocalDateTime createdAt,
    @Nullable LocalDateTime updatedAt,
    @NonNull String name,
    // ... other fields
) implements Serializable {}
```

**Step 2: Create Port Interface** (`domain/ports/`)
```java
public interface MyEntityService {
    @NonNull List<MyEntity> getAll();
    @NonNull MyEntity getById(@NonNull Long id) throws MyEntityNotFoundException;
    @NonNull MyEntity upsert(@NonNull MyEntity entity);
}
```

**Step 3: Create Service Implementation** (`domain/impl/`)
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MyEntityServiceImpl implements MyEntityService {
    private final MyEntityDataService dataService;
    
    @Override
    public @NonNull MyEntity getById(@NonNull Long id) {
        return dataService.getById(id);
    }
    // ... implement other methods
}
```

**Step 4: Create Data Port** (`domain/ports/`)
```java
public interface MyEntityDataService {
    @NonNull List<MyEntity> getAll();
    @NonNull MyEntity getById(@NonNull Long id) throws MyEntityNotFoundException;
    @NonNull MyEntity upsert(@NonNull MyEntity entity);
}
```

**Step 5: Create JPA Entity** (`data/persistence/`)
```java
@Entity
@Table(name = "my_entity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyEntityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "my_entity_seq_gen")
    @SequenceGenerator(name = "my_entity_seq_gen", sequenceName = "my_entity_seq", allocationSize = 1)
    private Long id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ... other fields
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
```

**Step 6: Create Repository** (`data/persistence/`)
```java
public interface MyEntityRepository extends JpaRepository<MyEntityEntity, Long> {
    void deleteAllInBatch();
    void flush();
    void resetSequence(); // Custom method for sequence reset
}
```

**Step 7: Create Entity Mapper** (`data/mapper/`)
```java
@Mapper(componentModel = "spring")
@ConditionalOnMissingBean
public interface MyEntityEntityMapper {
    MyEntity fromEntity(MyEntityEntity source);
    MyEntityEntity toEntity(MyEntity source);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(MyEntity source, @MappingTarget MyEntityEntity target);
}
```

**Step 8: Create Data Service Implementation** (`data/impl/`)
```java
@Service
@RequiredArgsConstructor
class MyEntityDataServiceImpl implements MyEntityDataService {
    private final MyEntityRepository repository;
    private final MyEntityEntityMapper mapper;
    
    @Override
    public @NonNull MyEntity getById(@NonNull Long id) {
        return repository.findById(id)
                .map(mapper::fromEntity)
                .orElseThrow(() -> new MyEntityNotFoundException(id));
    }
    // ... implement other methods
}
```

**Step 9: Create Flyway Migration** (`data/resources/db/migration/V2__create_my_entity_table.sql`)
```sql
SET TIME ZONE 'UTC';

CREATE SEQUENCE my_entity_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE my_entity (
    id bigint NOT NULL PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    name varchar(255) NOT NULL,
    -- ... other columns
);
```

**Step 10: Create DTO** (`api/dtos/`)
```java
@Builder(toBuilder = true)
public record MyEntityDto(
    @Nullable Long id,
    @Nullable LocalDateTime createdAt,
    @Nullable LocalDateTime updatedAt,
    @NonNull String name,
    // ... other fields
) {}
```

**Step 11: Create DTO Mapper** (`api/mapper/`)
```java
@Mapper(componentModel = "spring")
@ConditionalOnMissingBean
public interface MyEntityDtoMapper {
    MyEntityDto fromDomain(MyEntity source);
    MyEntity toDomain(MyEntityDto source);
}
```

**Step 12: Create Controller** (`api/controller/`)
```java
@Controller
@RequestMapping("/api/my-entity")
@RequiredArgsConstructor
public class MyEntityController {
    private final MyEntityService service;
    private final MyEntityDtoMapper mapper;
    
    @GetMapping("")
    public ResponseEntity<List<MyEntityDto>> getAll() {
        return ResponseEntity.ok(
            service.getAll().stream()
                .map(mapper::fromDomain)
                .toList()
        );
    }
    
    // ... other endpoints
}
```

**Step 13: Register Exception Handler** (`api/exceptions/GlobalExceptionHandler.java`)
```java
@ExceptionHandler({MyEntityNotFoundException.class})
public ResponseEntity<ErrorResponse> handleNotFoundException(...) {
    // Already handled by existing handler if it extends RuntimeException
}
```

### 2. Upsert Pattern

The `upsert` method handles both create and update:
- If `id == null`: Create new entity
- If `id != null`: Update existing entity (must exist)
- Timestamps managed by JPA lifecycle callbacks
- Returns persisted entity with populated ID and timestamps

### 3. Exception Translation

Data layer translates database exceptions to domain exceptions:
```java
catch (DataIntegrityViolationException e) {
    if (isDuplicateNameConstraintViolation(e)) {
        throw new DuplicatePosNameException(name);
    }
    throw e;
}
```

### 4. Mapper Patterns

**Domain ↔ Entity**: Handle structural differences (e.g., flat vs embedded address)
```java
@Mapping(source = "address.street", target = "street")
@Mapping(target = "houseNumber", expression = "java(mergeHouseNumber(source))")
Pos fromEntity(PosEntity source);
```

**Domain ↔ DTO**: Usually 1:1 mapping, but can add transformations if needed

---

## Adding New Features

### Checklist for New Features

1. ✅ **Domain Model**: Create immutable record with `@Builder(toBuilder = true)`
2. ✅ **Port Interface**: Define service contract in `domain/ports/`
3. ✅ **Service Implementation**: Implement business logic in `domain/impl/`
4. ✅ **Data Port**: Define data access contract in `domain/ports/`
5. ✅ **JPA Entity**: Create entity with timestamps and lifecycle callbacks
6. ✅ **Repository**: Extend `JpaRepository` with custom methods if needed
7. ✅ **Entity Mapper**: Map between domain and entity (handle structural differences)
8. ✅ **Data Service**: Implement data port, translate exceptions
9. ✅ **Flyway Migration**: Create SQL migration script
10. ✅ **DTO**: Create DTO record
11. ✅ **DTO Mapper**: Map between domain and DTO
12. ✅ **Controller**: Create REST endpoints
13. ✅ **Exception Handling**: Register exceptions in global handler
14. ✅ **Tests**: Write system tests

### Feature Implementation Order

Always implement **from the inside out**:
1. Domain layer first (models, ports, services)
2. Data layer (entities, repositories, mappers, implementations)
3. API layer (DTOs, mappers, controllers)
4. Tests

---

## Testing

### Test Structure
- **System Tests**: In `application/src/test/java/.../systest/`
- Extend `AbstractSysTest` for integration tests
- Use `TestFixtures` for test data creation
- Use `TestUtils` for HTTP requests

### Test Patterns
- Use AssertJ for assertions
- Ignore timestamp fields in comparisons: `.ignoringFields("createdAt", "updatedAt")`
- Use `usingRecursiveComparison()` for deep equality checks
- Create test data using domain models, then convert to DTOs for API calls

### Example Test
```java
@Test
void createPos() {
    Pos posToCreate = TestFixtures.getPosFixturesForInsertion().getFirst();
    Pos createdPos = posDtoMapper.toDomain(
        TestUtils.createPos(List.of(posDtoMapper.fromDomain(posToCreate))).getFirst()
    );
    
    assertThat(createdPos)
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(posToCreate);
}
```

---

## Development Workflow

### Building
```bash
mvn clean install
```

### Running Locally (Dev Profile)
1. Start PostgreSQL: `docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:17-alpine`
2. Run application: `cd application && mvn spring-boot:run -Dspring-boot.run.profiles=dev`

### Docker
1. Build JAR: `mvn clean install`
2. Build image: `docker compose build`
3. Run: `docker compose up`

### Database Migrations
- Migrations in `data/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Flyway runs migrations automatically on startup
- **Never modify existing migrations** - create new ones

---

## Important Notes

### ⚠️ Critical Rules

1. **Domain Layer Independence**
   - Domain layer must have **zero dependencies** on other layers
   - No Spring annotations in domain layer (except `@Service` in impl)
   - Domain models are pure Java records

2. **Immutable Domain Models**
   - Domain models are records (immutable)
   - Use `toBuilder()` to create modified copies
   - Never mutate domain objects directly

3. **Exception Handling**
   - Domain exceptions are thrown from domain layer
   - Data layer translates database exceptions to domain exceptions
   - API layer translates domain exceptions to HTTP responses

4. **Timestamp Management**
   - Timestamps are **NOT** set in domain layer
   - JPA lifecycle callbacks (`@PrePersist`, `@PreUpdate`) handle timestamps
   - Timestamps are in UTC

5. **Mapping Responsibilities**
   - Entity mappers handle structural differences (e.g., flat vs embedded address)
   - DTO mappers are usually 1:1 but can add transformations
   - Always use MapStruct, never manual mapping

6. **Database Constraints**
   - Unique constraints enforced at database level
   - Data layer catches `DataIntegrityViolationException` and translates to domain exceptions
   - Constraint names follow pattern: `{table}_{column}_key`

7. **Sequence Management**
   - Use sequences for ID generation
   - Reset sequences in `clear()` method for testing
   - Sequence naming: `{table}_seq`

### 🎯 Best Practices

1. **Logging**: Log important business operations at INFO level
2. **Validation**: Validate inputs in service layer, not just API layer
3. **Error Messages**: Provide clear, actionable error messages
4. **Documentation**: Add JavaDoc to public interfaces and complex methods
5. **Testing**: Write system tests for all CRUD operations
6. **Code Organization**: Follow the layer structure strictly

### 🔍 Common Gotchas

1. **House Number Parsing**: Domain uses string "21a", entity splits into numeric + suffix
2. **Timestamp Comparisons**: Always ignore timestamps in test assertions
3. **Upsert Logic**: Check if ID is null for create vs update
4. **Mapper Annotations**: Use `@ConditionalOnMissingBean` to prevent IntelliJ warnings
5. **Exception Translation**: Data layer must translate DB exceptions to domain exceptions
6. **Flyway**: Migration files must follow naming convention exactly

---

## Quick Reference

### Package Naming
- Domain models: `de.seuhd.campuscoffee.domain.model`
- Ports: `de.seuhd.campuscoffee.domain.ports`
- Services: `de.seuhd.campuscoffee.domain.impl`
- Entities: `de.seuhd.campuscoffee.data.persistence`
- Controllers: `de.seuhd.campuscoffee.api.controller`
- DTOs: `de.seuhd.campuscoffee.api.dtos`

### Common Annotations
- `@Service`: Service implementations
- `@Controller`: REST controllers
- `@Entity`: JPA entities
- `@Mapper`: MapStruct mappers
- `@Builder(toBuilder = true)`: Records with builder
- `@RequiredArgsConstructor`: Lombok constructor injection
- `@Slf4j`: Lombok logging
- `@NonNull` / `@Nullable`: JSpecify nullability

### HTTP Status Codes
- `200 OK`: Successful GET, PUT
- `201 Created`: Successful POST
- `400 Bad Request`: Validation errors, missing fields
- `404 Not Found`: Resource not found
- `409 Conflict`: Duplicate resources
- `500 Internal Server Error`: Unexpected errors

---

## Questions to Ask Before Implementing

1. **Which layer does this belong to?** (Domain, Data, API, Application)
2. **Does this require a new domain model?** (If yes, follow the full entity pattern)
3. **Are there structural differences between domain and persistence?** (Handle in mapper)
4. **What exceptions should this throw?** (Create domain exceptions, register in handler)
5. **Does this need a database migration?** (Create Flyway script)
6. **How should this be tested?** (Write system tests)

---

This guide should help you implement features that are consistent with the existing codebase architecture and patterns. When in doubt, refer to the existing `Pos` implementation as a reference.

