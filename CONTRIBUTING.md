# Contributing

## Development workflow

1. Create a focused branch from `main`.
2. Follow the existing feature-based layered architecture.
3. Add or update tests for behavior changes.
4. Run `mvn test`.
5. Keep commits small and use clear imperative commit messages.
6. Open a pull request explaining the change and how it was verified.

Do not commit credentials, `.env` files, generated build output, or application logs.

## Code conventions

- Use Java 17 language features conservatively.
- Keep controllers thin and business rules in services.
- Expose DTOs rather than JPA entities.
- Validate external input.
- Preserve structured API responses and centralized exception handling.
- Keep authorization rules explicit in `SecurityConfig`.
