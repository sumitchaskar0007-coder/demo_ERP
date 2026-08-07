const ACCOUNT_PASSWORD_PATTERN =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#_-])[A-Za-z\d@$!%*?&.#_-]{8,100}$/;

export function isValidAccountPassword(password: string) {
  return ACCOUNT_PASSWORD_PATTERN.test(password);
}

export function passwordRequirements(password: string) {
  return [
    { label: "8–100 characters", met: password.length >= 8 && password.length <= 100 },
    {
      label: "At least one uppercase and one lowercase letter",
      met: /[A-Z]/.test(password) && /[a-z]/.test(password),
    },
    { label: "At least one number", met: /\d/.test(password) },
    {
      label: "At least one special character: @ $ ! % * ? & . # _ -",
      met: /[@$!%*?&.#_-]/.test(password),
    },
    {
      label: "Only letters, numbers, and the special characters shown above",
      met: password.length > 0 && /^[A-Za-z\d@$!%*?&.#_-]+$/.test(password),
    },
  ];
}
